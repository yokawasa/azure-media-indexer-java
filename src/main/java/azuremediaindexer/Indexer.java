package azuremediaindexer;

import azuremediaindexer.Client;
import azuremediaindexer.Observer;
import azuremediaindexer.Subject;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.media.MediaContract;
import com.microsoft.windowsazure.services.media.models.Asset;
import com.microsoft.windowsazure.services.media.models.AssetFile;
import com.microsoft.windowsazure.services.media.models.AssetInfo;
import com.microsoft.windowsazure.services.media.models.AccessPolicyInfo;
import com.microsoft.windowsazure.services.media.models.AccessPolicy;
import com.microsoft.windowsazure.services.media.models.AccessPolicyPermission;
import com.microsoft.windowsazure.services.media.models.LocatorInfo;
import com.microsoft.windowsazure.services.media.models.LocatorType;
import com.microsoft.windowsazure.services.media.models.Locator;
import com.microsoft.windowsazure.services.media.models.ListResult;
import com.microsoft.windowsazure.services.media.models.MediaProcessorInfo;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;

public class Indexer extends Subject implements Runnable {
    private State state;
    private String assetName;
    private String taskParamFile;
    private String outputDir;
    private AssetInfo mediaAsset;
    private MediaContract service;
    private static final String taskXml = "<taskBody><inputAsset>JobInputAsset(0)</inputAsset><outputAsset>JobOutputAsset(0)</outputAsset></taskBody>";

    public Indexer(
            MediaContract service,
            String assetName,
            String taskParamFile,
            String outputDir) {
        this.state = new State();
        this.state.setValue("Initiating");
        this.service = service;
        this.assetName = assetName;
        this.taskParamFile = taskParamFile;
        this.outputDir = outputDir;
    }

    public Indexer( 
            MediaContract service,
            String assetName,
            String taskParamFile,
            String outputDir,
            String state,
            int progress) {
        this.state = new State();
        this.service = service;
        this.assetName = assetName;
        this.taskParamFile = taskParamFile;
        this.outputDir = outputDir;
        this.state.setValue(state);
        this.state.setProgress(progress);
    }

    @Override
    public void run() {
        try {
            String config = readFile(taskParamFile);
            if (config == null || config.equals("")) {
                System.err.println("Media Processor Task Param File cannot be empty:" + taskParamFile);
                System.exit(1);
            }

            MediaProcessorInfo mediaProcessor = Client.getIndexerMP();

            synchronized (service) {
                ListResult<AssetInfo> assets = service.list(Asset.list());
                for (AssetInfo asset : assets) {
                    if (asset.getName().equals(assetName)) {
                        mediaAsset = asset;
                    }
                }
            }

            // Create a task with the Indexer Media Processor
            Task.CreateBatchOperation task = Task.create(
                    mediaProcessor.getId(),taskXml)
                    .setConfiguration(config)
                    .setName(mediaAsset.getName() + "_Indexing");

            Job.Creator jobCreator = Job.create()
                    .setName(mediaAsset.getName() + "_Indexing")
                    .addInputMediaAsset(mediaAsset.getId())
                    .setPriority(2)
                    .addTaskCreator(task);

            final JobInfo jobInfo;
            final String jobId;
            synchronized (service) {
                jobInfo = service.create(jobCreator);
                jobId = jobInfo.getId();
            }
            checkJobStatus(jobId);

            downloadIndexedAssetFilesFromJob(jobInfo);
        } catch (Exception e) {
             System.err.println("Exception occured while running indexing job: "
                                        + e.getMessage());
            throw new RuntimeException(e.toString());
        }
    }

    private synchronized String readFile(String filePath) throws IOException {
        String content;
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
            line = br.readLine();
        }
        content = sb.toString();
        return content;
    }

    private synchronized void checkJobStatus(String jobId)
                throws ServiceException, InterruptedException {
        while (true) {
            JobInfo jobInfo = service.get(Job.get(jobId));
            JobState jobState = jobInfo.getState();
            LinkInfo<TaskInfo> tasksLink = service.get(Job.get(jobId)).getTasksLink();
            ListResult<TaskInfo> tasks = service.list(Task.list(tasksLink));
            this.state.setValue(jobState.name());
            this.state.setProgress((int)tasks.get(0).getProgress());
            notifyObservers(this.state);
            Thread.sleep(1000);

            if (
                jobState == JobState.Error 
                || jobState == JobState.Finished 
                || jobState == JobState.Canceled
            ) {
                if (jobInfo.getState() == JobState.Error) {
                    for (TaskInfo taskInfo : tasks) {
                        for (ErrorDetail detail : taskInfo.getErrorDetails()) {
                            System.err.println(
                                    String.format("TaskInfo Error: %s (code %s)",
                                                detail.getMessage(), detail.getCode()
                                            )
                                );
                        }
                    }
                }
                break;
            }
        }
    }

    private synchronized void downloadIndexedAssetFilesFromJob(JobInfo jobInfo)
            throws ServiceException, URISyntaxException, FileNotFoundException, StorageException, IOException {

        final ListResult<AssetInfo> outputAssets;
        outputAssets = service.list(Asset.list(jobInfo.getOutputAssetsLink()));
        AssetInfo indexedAsset = outputAssets.get(0);
        final AccessPolicyInfo downloadAccessPolicy;
        final LocatorInfo downloadLocator;

        downloadAccessPolicy = service.create(
                            AccessPolicy.create(
                                "Download",
                                15.0,
                                EnumSet.of(AccessPolicyPermission.READ)
                            )
                        );
        downloadLocator = service.create(
                            Locator.create(
                                downloadAccessPolicy.getId(),
                                indexedAsset.getId(),
                                LocatorType.SAS
                            )
                        );

        for (AssetFileInfo assetFile : service.list(AssetFile.list(indexedAsset.getAssetFilesLink()))) {
            String fileName = assetFile.getName();
            String outFileName=fileName;
            // Rename JobResult file not to overwrite it in the directory where all job output files are to be stored
            if (fileName.equals("JobResult.txt")) {
                outFileName = "JobResult_" + indexedAsset.getName();
            }
            String locatorPath = downloadLocator.getPath();
            int startOfSas = locatorPath.indexOf("?");
            String blobPath = locatorPath + fileName;
            if (startOfSas >= 0) {
                blobPath = locatorPath.substring(0, startOfSas) + "/" + fileName + locatorPath.substring(startOfSas);
            }
            URI baseUri = new URI(blobPath);
            CloudBlobClient blobClient = new CloudBlobClient(baseUri);
            String localFileName = this.outputDir + "/" + outFileName;
            CloudBlockBlob sasBlob = new CloudBlockBlob(baseUri);
            File fileTarget = new File(localFileName);
            sasBlob.download(new FileOutputStream(fileTarget));
        }

        service.delete(Locator.delete(downloadLocator.getId()));
        service.delete(AccessPolicy.delete(downloadAccessPolicy.getId()));
    }
}

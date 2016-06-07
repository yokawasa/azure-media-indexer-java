package azuremediaindexer;

import azuremediaindexer.Indexer;
import azuremediaindexer.StateListener;
import azuremediaindexer.Constants;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.media.MediaConfiguration;
import com.microsoft.windowsazure.services.media.MediaContract;
import com.microsoft.windowsazure.services.media.MediaService;
import com.microsoft.windowsazure.services.media.WritableBlobContainerContract;
import com.microsoft.windowsazure.services.media.models.Asset;
import com.microsoft.windowsazure.services.media.models.AssetFile;
import com.microsoft.windowsazure.services.media.models.AssetOption;
import com.microsoft.windowsazure.services.media.models.AssetInfo;
import com.microsoft.windowsazure.services.media.models.AccessPolicyInfo;
import com.microsoft.windowsazure.services.media.models.AccessPolicy;
import com.microsoft.windowsazure.services.media.models.AccessPolicyPermission;
import com.microsoft.windowsazure.services.media.models.LocatorInfo;
import com.microsoft.windowsazure.services.media.models.LocatorType;
import com.microsoft.windowsazure.services.media.models.Locator;
import com.microsoft.windowsazure.services.media.models.ListResult;
import com.microsoft.windowsazure.services.media.models.MediaProcessorInfo;
import com.microsoft.windowsazure.services.media.models.MediaProcessor;
import com.microsoft.windowsazure.services.blob.models.BlockList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream; 
import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.UUID;

public class Client {
    private MediaContract service;
    private static MediaProcessorInfo indexerMP;

    public Client(String amsaccount, String amskey) {

        this.service = MediaService.create(
                    MediaConfiguration.configureWithOAuthAuthentication(
                        Constants.MEDIA_SERVICE_URI,
                        Constants.OAUTH_URI,
                        amsaccount,
                        amskey,
                        Constants.URN
                    )
            );
    }

    public MediaContract getService() {
        return this.service;
    }

    public static MediaProcessorInfo getIndexerMP() {
        return indexerMP;
    }

    public void UploadFileAndCreateAsset(String uploadFile, String assetName)
            throws ServiceException,FileNotFoundException,NoSuchAlgorithmException,IOException {
        AssetInfo inputAsset = service.create(
                        Asset.create().
                            setName(assetName).
                            setOptions(AssetOption.None)
                     );
        AccessPolicyInfo writable = service.create(
                        AccessPolicy.create(
                            "writable",
                            10,
                            EnumSet.of(AccessPolicyPermission.WRITE)
                        )
                     );
        LocatorInfo assetBlobStorageLocator = service.create(
                        Locator.create(
                            writable.getId(),
                            inputAsset.getId(),
                            LocatorType.SAS
                        )
                     );
    
        WritableBlobContainerContract writer
                    = service.createBlobWriter(assetBlobStorageLocator);
        File mediaFile = new File(uploadFile);
        String fileName = mediaFile.getName();
        InputStream mediaFileInputStream = new FileInputStream(mediaFile);
        String blobName = fileName;
        
        // Upload the local file to the asset
        writer.createBlockBlob(fileName, null);
        String blockId;
        byte[] buffer = new byte[1024000];
        BlockList blockList = new BlockList();
        int bytesRead;
        ByteArrayInputStream byteArrayInputStream;
        while ((bytesRead = mediaFileInputStream.read(buffer)) > 0)
        {
            blockId = UUID.randomUUID().toString();
            byteArrayInputStream = new ByteArrayInputStream(buffer, 0, bytesRead); 
            writer.createBlobBlock(blobName, blockId, byteArrayInputStream);
            blockList.addUncommittedEntry(blockId);
        }
        writer.commitBlobBlocks(blobName, blockList);
        service.action(AssetFile.createFileInfos(inputAsset.getId()));
    }

    public void RunIndexingJob( String assetName, String taskParamFile, String outputDir ) 
                    throws InterruptedException, ServiceException {
        // Use latest media processor
        ListResult<MediaProcessorInfo> mediaProcessors
                = this.service.list(
                    MediaProcessor.list().set("$filter", "Name eq 'Azure Media Indexer'")
                  );

        for (MediaProcessorInfo info : mediaProcessors) {
            if (indexerMP == null
                    || info.getVersion().compareTo(indexerMP.getVersion()) > 0 
            ) {
                indexerMP = info;
            }
        }
        System.out.println("Using MediaProcessor: " + indexerMP.getName() + " " + indexerMP.getVersion());

        final StateListener listener = new StateListener(assetName);
        Indexer videoIndexer = new Indexer(
                                    this.service,
                                    assetName,
                                    taskParamFile,
                                    outputDir);
        videoIndexer.addObserver(listener);

        Thread indexingThread = new Thread(videoIndexer);

        indexingThread.setUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(Thread t, Throwable e) {}
                }
            );
        indexingThread.start();
        PrintStateProgress(listener);
    }
    
    private void PrintStateProgress(StateListener listener)
                                                throws InterruptedException {
        while(true) {
            if (listener.state.getValue().equals("Finished") 
                    || listener.state.getValue().equals("Error") 
                    || listener.state.getValue().equals("Canceled")
            ) {
                break;
            }
            String statusOutput = String.format("Indexing: %s [%s] Progress %d Percent",
                                    listener.name,
                                    listener.state.getValue(),
                                    listener.state.getProgress());
            System.out.println(statusOutput);
            Thread.sleep(2000);
        }
    }
}

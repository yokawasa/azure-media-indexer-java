package azuremediaindexer;

import azuremediaindexer.Client;
import azuremediaindexer.PropertyLoader;
import com.microsoft.windowsazure.exception.ServiceException;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Hashtable;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class App 
{

    public static void main( String[] args )
    {
        new App().start(args);
    }

    public void start( String[] args )
    {
        String conffile = null;
        String uploadfile = null;
        String assetname = null;
        String paramfile = null;
        String outputdir = null;

        Options opts = new Options();
        opts.addOption("c", "config", true, "App config file. ex) app.config");
        opts.addOption("f", "file", true, "Uploading file. By specifing this, you start from uploading file");
        opts.addOption("a", "assetname", true, "Asset Name to process media indexing");
        opts.addOption("p", "params", true, "Azure media indexer task parameter file. ex) default-ami.config");
        opts.addOption("o", "output", true, "Output directory");
        BasicParser parser = new BasicParser();
        CommandLine cl;
        HelpFormatter help = new HelpFormatter();

        try {
            // parse options
            cl = parser.parse(opts, args);
            // handle server option.
            if ( !cl.hasOption("-c") || !cl.hasOption("-a") || !cl.hasOption("-p") || !cl.hasOption("-o")) {
                throw new ParseException("");
            }
            // handle interface option.
            conffile = cl.getOptionValue("c");
            uploadfile = cl.getOptionValue("f");
            assetname = cl.getOptionValue("a");
            paramfile = cl.getOptionValue("p");
            outputdir = cl.getOptionValue("o");
            if (conffile == null || assetname == null || paramfile == null || outputdir == null) {
                throw new ParseException("");
            }
            // handlet destination option.
            System.out.println("Starting application...");
            System.out.println("Config file :" + conffile);
            System.out.println("AMS Account :" + PropertyLoader.getInstance(conffile)
                                                    .getValue("MediaServicesAccountName"));
            if (cl.hasOption("-f")) { 
                System.out.println("Uploading file : " + uploadfile);
            }
            System.out.println("Asset name : " + assetname);
            System.out.println("Task param file : " + paramfile);
            System.out.println("Output dir : " + outputdir);

        } catch (IOException | ParseException e) {
            help.printHelp("App Args Errors:", opts);
            System.exit(1);
        }
       
        // create client instance 
        Client client = null;

        try {
            client  = new Client(
                        PropertyLoader.getInstance(conffile).getValue("MediaServicesAccountName"),
                        PropertyLoader.getInstance(conffile).getValue("MediaServicesAccountKey")
                    );
        } catch ( IOException e){
            System.err.println("Client initializing failure:" + e);
            System.exit(1);
        }

        // upload if opted
        if (uploadfile != null ) { 
            try {
                client.UploadFileAndCreateAsset(uploadfile, assetname);
            } catch ( Exception e ){
                System.err.println("Video upload failure:" + e);
                System.exit(1);
            }
        }

        // index asset
        try {
            client.RunIndexingJob(assetname, paramfile, outputdir);
        } catch ( Exception e ){
            System.err.println("Video indexing failure:" + e);
            System.exit(1);
        }
    }
}

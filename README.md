# Azure Media Indexer Java Client Implementation

This repository contains Java codes that implement Azure Media Indexer Client using Azure SDK for Java.

## How to compile, run, and install

    # compiling
    mvn compile
    
    # executing
    mvn exec:java -Dexec.args="-a SampleAsset -c ./app.config -f ./sample.mp4 -p ./default-ami.config -o /path/output"
    
    # packaging
    mvn package
    
    # Installing. The project's runtime dependencies copied into the target/lib folder
    mvn install

## Application Configuration
app.config

     MediaServicesAccountName=<Azure Media Service Account Name>
     MediaServicesAccountKey=<Azure Media Service Account Key>

## Configuration for Media Indexer Processing
default-ami.config

    <?xml version="1.0" encoding="utf-8" ?>
    <configuration version="2.0">
    <input>
       <!-- [Optional] [Recommended] Metadata of the input media file(s) -->
       <metadata key="title" value="Azure Media Indexer Demo Video" />
       <metadata key="description" value="Azure Media Indexer Demo Video" />
    </input>
    <settings>
       <!-- Reserved -->
    </settings>
      
    <!--new stuff starts here-->
    <features>
       <feature name="ASR">
         <settings>
           <add key="Language" value="English"/>
           <add key="CaptionFormats" value="ttml;webvtt"/>
           <add key="GenerateAIB" value ="false" />
           <add key="GenerateKeywords" value ="true" />
         </settings>
       </feature>
    </features>
    <!--new stuff ends here-->
    </configuration>

## Application Usage 
Here is how to execute the application using mvn command:

    mvn exec:java -Dexec.args="-a SampleAsset -c ./app.config -f ./sample.mp4 -p ./default-ami.config -o /path/output"

Here are args for the application that you specify in running the app:

    usage: App Usage:
     -a,--assetname <arg>   Asset Name to process media indexing
     -c,--config <arg>      App config file. ex) app.config
     -f,--file <arg>        Uploading file. By specifing this, you start from uploading file
     -o,--output <arg>      Output directory
     -p,--params <arg>      Azure media indexer task parameter file. ex) default-ami.config


## TODOs
 * Add unit testing code

## Contributing

Bug reports and pull requests are welcome on GitHub at https://github.com/yokawasa/azure-media-indexer-java.


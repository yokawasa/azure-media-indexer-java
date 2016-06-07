# Azure Media Indexer Java Client Implementation

This repository contains Java codes that implement Azure Media Indexer Client using Azure SDK for Java.

## How to compile, run, and install

    # compiling
    mvn compile
    
    # executing
    mvn exec:java -Dexec.args="-a YourAssetName -c ./app.config -f ./sample.mp4 -p ./default-ami.config -o /path/output"
    
    # packaging
    mvn package
    
    # Installing. The project's runtime dependencies copied into the target/lib folder
    mvn install

## Configuration

### Azure Media Services

In order to get started using Azure Media Indexer, you must create an Azure Media Service Account in the Azure Portal and obtain Azure Media Service Accoutn Name and Account Key information that you need to set in your application config file. Here are instructions:

 * [Create an Azure Media Services account](https://azure.microsoft.com/en-us/documentation/articles/media-services-create-account/)


### Application Configuration
**app.config**

     MediaServicesAccountName=<Azure Media Service Account Name>
     MediaServicesAccountKey=<Azure Media Service Account Key>

### Configuration for Media Indexer Processing Task
**default-ami.config**

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

    mvn exec:java -Dexec.args="-a YourAssetName -c ./app.config -f ./sample.mp4 -p ./default-ami.config -o /path/output"

Here are args for the application that you specify in running the app:

     usage: App -c <app.config> [-f <uploadfile>] -a <assetname> -p <amitaskparam.config> -o <outputdir>
     -a,--assetname <arg>   (Required) Asset Name to process media indexing
     -c,--config <arg>      (Required) App config file. ex) app.config
     -f,--file <arg>        (Optional) Uploading file. By specifing this, you start from uploading file
     -o,--output <arg>      (Required) Output directory
     -p,--params <arg>      (Required) Azure media indexer task parameter file. ex) default-ami.config

## TODOs
 * Add unit testing code
 * Write some blog article describing How-To on this app

## Contributing

Bug reports and pull requests are welcome on GitHub at https://github.com/yokawasa/azure-media-indexer-java.

## Copyright

<table>
  <tr>
    <td>Copyright</td><td>Copyright (c) 2016- Yoichi Kawasaki</td>
  </tr>
  <tr>
    <td>License</td><td>The MIT License (MIT)</td>
  </tr>
</table>



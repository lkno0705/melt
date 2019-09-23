package de.uni_mannheim.informatik.dws.ontmatching.matchingexternal;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatcherExternalCommandFromFile extends MatcherExternal {

    protected Path filePath = Paths.get("external", "external_command.txt");
    
    protected static String xmx = "";
    protected static String xms = "";
    
    static{
        for(String s : ManagementFactory.getRuntimeMXBean().getInputArguments()){
            if(s.toLowerCase().contains("xmx")){
                xmx = s;
            }else if(s.toLowerCase().contains("xms")){
                xms = s;
            }
        }
    }
    
    @Override
    protected List<String> getCommand(URL source, URL target, URL inputAlignment) throws Exception {
        String fileContent = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        List<String> command = new ArrayList(Arrays.asList(replaceString(fileContent).split(" ")));
        command.add(source.toString());
        command.add(target.toString());
        if(inputAlignment != null)
            command.add(inputAlignment.toString());
        return command;
    }
    
    protected String replaceString(String s){
        return s.replace("\r", "").replace("\n", "")
                .replace("{File.pathSeparator}", File.pathSeparator)
                .replace("{File.separator}", File.separator)
                .replace("{xmx}", xmx)
                .replace("{xms}", xms)
                .trim();
    }
}

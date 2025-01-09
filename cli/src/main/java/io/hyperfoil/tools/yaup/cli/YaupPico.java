package io.hyperfoil.tools.yaup.cli;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.JsonComparison;
import io.hyperfoil.tools.yaup.json.JsonValidator;
import io.hyperfoil.tools.yaup.xml.XmlLoader;
import io.hyperfoil.tools.yaup.xml.XmlOperation;
import io.hyperfoil.tools.yaup.xml.pojo.Xml;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.reader.StreamReader;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

//@TopCommand
@QuarkusMain
@CommandLine.Command(name="", mixinStandardHelpOptions = true, subcommands={CommandLine.HelpCommand.class, AutoComplete.GenerateCompletion.class})
public class YaupPico implements QuarkusApplication {
    final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    //currently not used?
    @Override
    public int run(String... args) throws Exception {
        System.setProperty("polyglotimpl.DisableClassPathIsolation", "true");
        CommandLine cmd = new CommandLine(new YaupPico());
        CommandLine gen = cmd.getSubcommands().get("generate-completion");
        gen.getCommandSpec().usageMessage().hidden(true);
        return cmd.execute(args);
    }
    @CommandLine.Command(name="xml", description="perform the operation on an xml document", mixinStandardHelpOptions = true)
    public int xml(@CommandLine.Option(names={"-j","--json"}) boolean json, String operation, @CommandLine.Parameters(arity = "0..1") String path){
        Xml doc;
        if(path==null || path.isBlank()){
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))){
                String allLines = reader.lines().collect(Collectors.joining("\n"));
                doc = Xml.parse(allLines);
            } catch (IOException e) {
                return 1;
            }
        } else {
            doc = Xml.parseFile(path);
        }
        String response = doc.apply(operation);
        logger.infof("xml response:%n%s",response);
        if(json ){
            Xml found = Xml.parse(response);
            if( found.exists()){
                response = found.toJson().toString();
            }
        }
        System.out.printf("%s%n", response);
        return 0;
    }
    @CommandLine.Command(name="json-diff", description = "compare two json objects")
    public int jsonDiff(String firstPath,String secondPath){
        Json firstJson = Json.fromFile(firstPath);
        Json secondJson = Json.fromFile(secondPath);

        JsonComparison comparison = new JsonComparison();
        comparison.load("A",firstJson);
        comparison.load("B",secondJson);

        int length = comparison.jsonNames().stream().mapToInt(String::length).max().orElseGet(()->1);
        comparison.getDiffs().forEach(entry->{
            System.out.printf("%s%n",entry.getPath());
            entry.forEach((key,value)->{
                System.out.printf("%"+length+"s : %s%n",key,value.length() < 40 ? value : value.substring(0,40)+"...");
            });
        });
        return 0;
    }
    @CommandLine.Command(name="json-schema", description="validate json with a schema",aliases = {"schema"})
    public int jsonSchema(String schemaPath,String jsonPath){
        Json schemaJson = Json.fromFile(schemaPath);
        Json dataJson = Json.fromFile(jsonPath);
        JsonValidator validator = new JsonValidator(schemaJson);
        Json errors = validator.validate(dataJson);
        logger.info(errors.toString());
        System.out.printf("%s%n", errors.toString(2));
        return 0;
    }
    @CommandLine.Command(name="type-structure", description="create a type structure from the provided json files",aliases = {"shape","typestructure","structure"})
    public int structure(List<String> paths){
        if(paths == null || paths.isEmpty()){
            logger.errorf("missing required paths to json files");
            return 1;
        }
        Json structure = null;
        for(String path : paths){
            Json loaded = Json.fromFile(path);
            Json loadedStructure = Json.typeStructure(loaded);
            if(structure == null){
                structure = loadedStructure;
            }else{
                structure.add(loadedStructure);
            }
        }
        if(structure == null){
            logger.errorf("failed to load structure");
            return 1;
        }else{
            System.out.printf(structure.toString(2));
        }
        return 0;
    }

}

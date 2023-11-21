package io.hyperfoil.tools.yaup.yaml;

import io.hyperfoil.tools.yaup.StringUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

public class PatternTemplate {

    public static class Construct extends DeferableConstruct {
        @Override
        public Object construct(Node node) {
            if(node instanceof ScalarNode){
                ScalarNode scalarNode = (ScalarNode) node;
                String value = scalarNode.getScalarStyle().equals(DumperOptions.ScalarStyle.DOUBLE_QUOTED) ? StringUtil.quote(scalarNode.getValue(),"\"")
                        : scalarNode.getScalarStyle().equals(DumperOptions.ScalarStyle.SINGLE_QUOTED) ? StringUtil.quote(scalarNode.getValue(),"'")
                        : scalarNode.getValue();
                return new PatternTemplate(value);
            }else {
                throw new YAMLException("patterns require scalar value "+node.getStartMark());
            }
        }
    }
    private String prefix;
    private String suffix;
    private String javascriptPrefix;
    private String separator;
    private String template;
    private boolean isQuoted=false;
    public PatternTemplate(String template){
        this(template, StringUtil.PATTERN_PREFIX,StringUtil.PATTERN_SUFFIX,StringUtil.PATTERN_DEFAULT_SEPARATOR,StringUtil.PATTERN_JAVASCRIPT_PREFIX);
    }
    public PatternTemplate(String template,String prefix,String suffix,String separator,String javascriptPrefix){
        this.template = StringUtil.removeQuotes(template);
        this.isQuoted = (StringUtil.isQuoted(template));
        this.prefix = prefix;
        this.suffix = suffix;
        this.separator = separator;
        this.javascriptPrefix = javascriptPrefix;
    }
    public String getPrefix() {
        return prefix;
    }
    public String getSuffix() {
        return suffix;
    }
    public String getJavascriptPrefix() {
        return javascriptPrefix;
    }
    public String getSeparator() {
        return separator;
    }
    public String getTemplate() {
        return template;
    }
    @Override
    public String toString(){
        return template;
    }
}

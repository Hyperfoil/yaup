package perf.yaup;

import java.util.List;
import java.util.function.Function;

/**
 * Created by wreicher
 */
public class AsciiArt {

    public static final String ANSI_RESET = "\u001B[0m";

    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_MAGENTA = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_LIGHT_GREY = "\u001B[37m";
    public static final String ANSI_DARK_GREY = "\u001B[90m";
    public static final String ANSI_LIGHT_RED = "\u001B[91m";
    public static final String ANSI_LIGHT_GREEN = "\u001B[92m";
    public static final String ANSI_LIGHT_YELLOW = "\u001B[93m";
    public static final String ANSI_LIGHT_BLUE = "\u001B[94m";
    public static final String ANSI_LIGHT_MAGENTA = "\u001B[95m";
    public static final String ANSI_LIGHT_CYAN = "\u001B[96m";
    public static final String ANSI_WHITE = "\u001B[97m";

    private static String KMG_SUFFIX = "KMGTPEZY";
    public static String CHECKED = "✓";
    public static String XED = "✕";
    public static String BOX = "☐";
    public static String BOX_CHECKED ="☑";
    public static String BULLET = "◦";
    public static String HORIZ_SCALE = " ▁▂▃▄▅▆▇█";
    public static String VERT_SCALE =  " ▏▎▍▌▋▊▉█";
    public static String TRIANGLE_RIGHT = "▶";
    public static String TRIANGLE_LEFT = "◀";
    public static String DOUBLE_RIGHT = "≫";
    public static String DOUBLE_LEFT = "≪";
    public static String TREE_OFFSET_SPACE =     "   ";
    public static String TREE_OFFSET_SUB_CHILD = " │ ";
    public static String TREE_CHILD =            " ├ ";
    public static String TREE_CHILD_LAST =       " └ ";
    public static String ELLIPSIS = "…";

    public static String printKMG(double bytes){
        return printKMG(bytes,2);
    }
    public static String printKMG(double bytes,int precision){
        int index = -1;
        double amnt = bytes;
        while(amnt >= 1024 && index < KMG_SUFFIX.length()-1){
            amnt = amnt / 1024;
            index++;
        }
        return String.format("%."+precision+"f%sB",amnt,(index>-1)?KMG_SUFFIX.charAt(index):"");
    }

    public static String termGoto(int row,int column){
        return "\u001b["+row+";"+column+"H";
        //return "\u001B["+row+";"+column+"H";
    }



    public static char horiz(double value){
        return horiz(value,100);
    }
    public static char horiz(double value,double maxValue){
        double hIdx = ( 1.0*value ) / maxValue * (HORIZ_SCALE.length()-1);
        return HORIZ_SCALE.charAt((int)hIdx);
    }
    public static String vert(double value){
        return vert(value,100,13,true);
    }
    public static String vert(double value,double maxValue,int width,boolean fill){
        StringBuilder rtrn = new StringBuilder();
        int denom = VERT_SCALE.length()*width;
        double sizer = maxValue / (1.0*denom);
        int vIdx = (int)Math.floor( ( (value) / sizer ) )% (VERT_SCALE.length()) ;
        int vSp = (int)( (value) / sizer ) / (VERT_SCALE.length());

        for(int i=0; i<width; i++){
            if(i<vSp){
                rtrn.append(VERT_SCALE.charAt(VERT_SCALE.length()-1));
            }else if (i==vSp){
                rtrn.append(VERT_SCALE.charAt( vIdx ));
            }else {
                if(fill) {
                    rtrn.append(VERT_SCALE.charAt(0));
                }else{
                    return rtrn.toString();
                }
            }
        }
        return rtrn.toString();
    }

    protected static <T> void printTreeAppend(T target, Function<T,List<T>> getChildren,Function<T,String> printFunction,StringBuilder sb,String prefix,boolean isTopLevel,boolean isLastChild){
        sb.append(prefix);
        if(!isTopLevel){ //only the top level doesn't have a prefix
            if(isLastChild){
                sb.append(TREE_CHILD_LAST);
            }else{
                sb.append(TREE_CHILD);
            }
        }
        if(printFunction==null) {
            sb.append(target.toString());
        }else {
            sb.append(printFunction.apply(target));
        }
        List<T> children = getChildren.apply(target);
        if(!children.isEmpty()){
            String newChildPrefix = prefix+TREE_OFFSET_SUB_CHILD;
            String newSpacePrefix = prefix+TREE_OFFSET_SPACE;

            for(int i=0; i<children.size(); i++){
                T child = children.get(i);
                sb.append(System.lineSeparator());
                String newPrefix = (isTopLevel) ? "" : (isLastChild) ? newSpacePrefix : newChildPrefix;
                printTreeAppend(child, getChildren, printFunction, sb, newPrefix, false, i==children.size()-1);
            }
        }
    }
    public static <T> String printTree(T target, Function<T,List<T>> getChildren){
        return printTree(target,getChildren,null);
    }
    public static <T> String printTree(T target, Function<T,List<T>> getChildren,Function<T,String> printFunction){
        StringBuilder rtrn = new StringBuilder();
        printTreeAppend(target,getChildren,printFunction,rtrn,"",true, true);
        return rtrn.toString();
    }
    public static String abLine(double a,double b,double max,int width,boolean fill){
        String both="█";
        String top="▀";
        String bot="▄";
        StringBuilder rtrn = new StringBuilder();

        double denom = 1.0*max / width;
        int aWidth = (int)(1.0*a / max * width);
        int bWidth = (int)(1.0*b / max * width);

        for(int i=1; i<=width;i++){
            if(i<=aWidth && i<=bWidth){
                rtrn.append(both);
            }else if (i<=aWidth){
                rtrn.append(top);
            }else if (i<=bWidth){
                rtrn.append(bot);
            }else{
                if(fill) {
                    rtrn.append(" ");
                }else {
                    return rtrn.toString();
                }
            }
        }

        return rtrn.toString();
    }

}

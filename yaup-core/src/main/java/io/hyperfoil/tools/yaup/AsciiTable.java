package io.hyperfoil.tools.yaup;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AsciiTable {
    public static final String HEADER_TOP_LEFT = "HTL";
    public static final String HEADER_TOP_RIGHT = "HTR";
    public static final String HEADER_TOP_INTERSECT = "HTI";
    public static final String HEADER_COLUMN_SEPARATOR = "HCS";
    //public static final String HEADER_BORDER_VERTICAL = "HBV";
    public static final String HEADER_BORDER_LEFT = "HBL";
    public static final String HEADER_BORDER_RIGHT = "HBR";
    public static final String HEADER_BORDER_HORIZONTAL = "HBH";
    public static final String TABLE_TOP_LEFT = "TTL";
    public static final String TABLE_TOP_RIGHT = "TTR";
    public static final String TABLE_TOP_HORIZONTAL= "TTH";
    public static final String TABLE_TOP_INTERSECT = "TTI";
    //public static final String TABLE_BORDER_VERTICAL = "TBV";
    public static final String TABLE_BORDER_LEFT = "TSL";
    public static final String TABLE_BORDER_RIGHT = "TSR";
    public static final String TABLE_COLUMN_SEPARATOR = "TCS";
    public static final String ROW_SEPARATOR_LEFT = "RSL";
    public static final String ROW_SEPARATOR_HORIZONTAL = "RSH";
    public static final String ROW_SEPARATOR_INTERSECT = "RSI";
    public static final String ROW_SEPARATOR_RIGHT = "RSR";
    public static final String TABLE_BOTTOM_LEFT = "TBL";
    public static final String TABLE_BOTTOM_RIGHT = "TBR";
    public static final String TABLE_BOTTOM_INTERSECT = "TBI";
    public static final String TABLE_BOTTOM_HORIZONTAL = "TBH";
    public static final String BANNER_TOP_LEFT = "BTL";
    public static final String BANNER_BORDER_HORIZONTAL = "BTH";
    public static final String BANNER_TOP_RIGHT = "BTR";
    //public static final String BANNER_BORDER_VERTICAL = "BTV";
    public static final String BANNER_BORDER_LEFT = "BBL";
    public static final String BANNER_BORDER_RIGHT = "BBR";
    //public static final String FOOTER_BORDER_VERTICAL = "FBV";
    public static final String FOOTER_BORDER_LEFT = "FBL";
    public static final String FOOTER_BORDER_RIGHT = "FBR";
    public static final String FOOTER_COLUMN_SEPARATOR = "FCS";
    public static final String FOOTER_BORDER_HORIZONTAL = "FBH";

    public static final String POSTGRES_TEMPLATE=
           """
            h|h\s
           --+--
            v|v\s
           """;
    public static final String SQLITE_TEMPLATE=
            """
            +-+-+
            |h|h|
            +-+-+
            |v|v|
            +-+-+
            """;
    public static final String DUCKDB_TEMPLATE=
            """
            ┌─┬─┐
            │h│h│
            ├─┼─┤
            │v│v│
            └─┴─┘
            """;
    public static final String DOUBLE_BORDER_TEMPLATE=
            """
            ╔═╦═╗
            ║h║h║
            ╠═╬═╣
            ║v║v║
            ╚═╩═╝
            """;
    public static final String COMPACT_TEMPLATE=
            """
            ┌───┐
            │h h│
            ├───┤
            │v v│
            └───┘
            """;

    /*
     *
     *       HBV  h  HCS  h  HBV
     *       TTL TTH TTI TTH TTR
     *       TBV  v  TCS  v  TBV
     *
     *       HTL HBH HTI HBH HTR
     *       HBV  h  HCS  h  HBV
     *       TTL TTH TTI TTH TTR
     *       TBV  v  TCS  v  TBV
     *       TBL TBH TBI TBH TBR
     *
     *       HTL HBH HTI HBH HTR
     *       HBV  h  HCS  h  HBV
     *       TTL TTH TTI TTH TTR
     *       TBV  v  TCS  v  TBV
     *       RSL RSH RSI RSH RSR
     *       TBL TBH TBI TBH TBR
     *
     *       idea?
     *       BTL BBH BBH BBH BTR    banner top
     *       BBV      b      BBV    banner (calling it title would collide with table in three letter key)
     *       HTL HBH HTI HBH HTR    header top
     *       HBV  h  HCS  h  HBV    header
     *       TTL TTH TTI TTH TTR    table header
     *       TBV  v  TCS  v  TBV    value (row)
     *       RSL RSH RSI RSH RSR    row separator
     *       TBL TBH TBI TBH TBR    table bottom
     *       FBV      f      FBV    footer
     *       FBL FBH FBH FBH FBR    footer border
     *
     */

    /*
     * splits the row of input strings by line separator and returns a list of top aligned rows for each line in the headers.
     * [ "foo\nfoo","bar" ] -> [ [ "foo","bar" ] , [ "foo","" ] ]
     * @param input
     * @return
     */
    public static List<List<String>> lineSplit(List<String> input){
        List<List<String>> split = input.stream().map(line-> List.of(line.split(System.lineSeparator()))).toList();
        int maxRows = split.stream().mapToInt(List::size).max().orElse(1);
        List<List<String>> rtrn = new ArrayList<>();
        for(int i=0; i<maxRows; i++){
            ArrayList<String> row = new ArrayList<>();
            for(List<String> splitRow : split){
                if(splitRow.size()>i){
                    row.add(splitRow.get(i));
                }else{
                    row.add("");
                }
            }
            rtrn.add(row);
        }
        return rtrn;
    }


    private final Map<String,String> characters;
    private final Map<String,String> prefixes;
    private final Map<String,String> suffixes;
    private int precision;
    private String prefixAll;
    private String suffixAll;
    //per column customization
    private Map<Integer,String> headerTopIntersect;
    private Map<Integer,String> headerColumnSeparators;

    private Map<Integer,String> tableTopIntersect;
    private Map<Integer,String> tableColumnSeparators;
    private Map<Integer,String> tableBottomIntersect;
    private Map<Integer,String> tableColumnFormats;
    private Map<Integer,String> rowSeparatorIntersect;





    public AsciiTable(){
        this.characters = new HashMap<>();
        this.prefixes = new HashMap<>();
        this.suffixes = new HashMap<>();
        this.prefixAll = null;
        this.suffixAll = null;
        this.headerTopIntersect = new HashMap<>();
        this.headerColumnSeparators = new HashMap<>();
        this.tableTopIntersect = new HashMap<>();
        this.tableColumnSeparators = new HashMap<>();
        this.tableBottomIntersect = new HashMap<>();
        this.rowSeparatorIntersect = new HashMap<>();
        this.tableColumnFormats = new HashMap<>();
        this.precision = 2;
    }

    public AsciiTable(String template) {
        this();
        String split[] = template.split(System.lineSeparator());
        /*
         *       HBV  h  HCS  h  HBV
         *       TTL TTH TTI TTH TTR
         *       TBV  v  TCS  v  TBV
         */
        if (split.length == 3) {
            headerLine(split[0]);
            tableTop(split[1]);
            tableLine(split[2]);
            /*
             *       HTL HBH HTI HBH HTR
             *       HBV  h  HCS  h  HBV
             *       TTL TTH TTI TTH TTR
             *       TBV  v  TCS  v  TBV
             *       TBL TBH TBI TBH TBR
             */
        } else if (split.length == 5) {
            headerTop(split[0]);
            headerLine(split[1]);
            tableTop(split[2]);
            tableLine(split[3]);
            tableBottom(split[4]);
            String line = split[0];
            /*
             *       HTL HBH HTI HBH HTR
             *       HBV  h  HCS  h  HBV
             *       TTL TTH TTI TTH TTR
             *       TBV  v  TCS  v  TBV
             *       RSL RSH RSI RSH RSR
             *       TBL TBH TBI TBH TBR
             */
        } else if (split.length == 6) {
            headerTop(split[0]);
            headerLine(split[1]);
            tableTop(split[2]);
            tableLine(split[3]);
            rowSeparator(split[4]);
            tableBottom(split[5]);
        }
    }

    public AsciiTable setColumnFormat(int column,String format){
        tableColumnFormats.put(column,format);
        return this;
    };

    public boolean isValid(){
        return Stream.of(HEADER_COLUMN_SEPARATOR, TABLE_TOP_HORIZONTAL, TABLE_TOP_INTERSECT,TABLE_COLUMN_SEPARATOR).allMatch(characters::containsKey);
    }

    public AsciiTable setPrecision(int precision){
        this.precision = precision;
        return this;
    }

    /* BTL BBH BTR
     * BTL BBH... BTR
     */
    public AsciiTable bannerTop(String line){
        if(line.length()==3){
            characters.put(BANNER_TOP_LEFT,line.charAt(0)+"");
            characters.put(BANNER_BORDER_HORIZONTAL,line.charAt(1)+"");
            characters.put(BANNER_TOP_RIGHT,line.charAt(2)+"");
        }else if(line.length()>4){
            characters.put(BANNER_TOP_LEFT,line.charAt(0)+"");
            characters.put(BANNER_BORDER_HORIZONTAL,line.charAt(1)+"");
            characters.put(BANNER_TOP_RIGHT,line.charAt(line.length()-1)+"");
        }
        return this;
    }
    /*
     * BBV      b      BBV
     */
    public AsciiTable bannerLine(String line){
        characters.put(BANNER_BORDER_LEFT,line.charAt(0)+"");
        characters.put(BANNER_BORDER_RIGHT,line.charAt(line.length()-1)+"");
        return this;
    }
    /* HTL HBH HTR
     * HTL HBH HTI HBH HTR
     */
    public AsciiTable headerTop(String line){
        if(line.length()==3){
            characters.put(HEADER_TOP_LEFT, line.charAt(0) + "");
            characters.put(HEADER_BORDER_HORIZONTAL, line.charAt(1) + "");
            characters.put(HEADER_TOP_INTERSECT, line.charAt(1) + "");
            characters.put(HEADER_TOP_RIGHT, line.charAt(2) + "");
        }else if (line.length()>4){
            characters.put(HEADER_TOP_LEFT, line.charAt(0) + "");
            characters.put(HEADER_BORDER_HORIZONTAL, line.charAt(1) + "");
            characters.put(HEADER_TOP_INTERSECT, line.charAt(2) + "");
            characters.put(HEADER_TOP_RIGHT, line.charAt(line.length()-1) + "");
        }
        return this;
    }
    /*
     * HBV  h  HCS  h  HBV
     */
    public AsciiTable headerLine(String line){
        characters.put(HEADER_BORDER_LEFT, line.charAt(0) + "");
        characters.put(HEADER_COLUMN_SEPARATOR, line.charAt(2) + "");
        characters.put(HEADER_BORDER_RIGHT, line.charAt(line.length()-1)+"");
        return this;
    }
    /*
     * TTL TTH TTI TTH TTR
     */
    public AsciiTable tableTop(String line){
        characters.put(TABLE_TOP_LEFT, line.charAt(0) + "");
        characters.put(TABLE_TOP_HORIZONTAL, line.charAt(1) + "");
        characters.put(TABLE_TOP_INTERSECT, line.charAt(2) + "");
        characters.put(TABLE_TOP_RIGHT, line.charAt(4) + "");
        return this;
    }
    /*
     * TBL  v  TCS  v  TBR
     */
    public AsciiTable tableLine(String line){
        characters.put(TABLE_BORDER_LEFT,line.charAt(0)+"");
        characters.put(TABLE_COLUMN_SEPARATOR,line.charAt(2)+"");
        characters.put(TABLE_BORDER_RIGHT,line.charAt(line.length()-1)+"");
        return this;
    }
    /*
     * RSL RSH RSI RSH RSR
     */
    public AsciiTable rowSeparator(String line){
        characters.put(ROW_SEPARATOR_LEFT,line.charAt(0)+"");
        characters.put(ROW_SEPARATOR_HORIZONTAL,line.charAt(1)+"");
        characters.put(ROW_SEPARATOR_INTERSECT,line.charAt(2)+"");
        characters.put(ROW_SEPARATOR_RIGHT,line.charAt(4)+"");
        return this;
    }
    /*
     * TBL TBH TBI TBH TBR
     */
    public AsciiTable tableBottom(String line){
        characters.put(TABLE_BOTTOM_LEFT,line.charAt(0)+"");
        characters.put(TABLE_BOTTOM_HORIZONTAL,line.charAt(1)+"");
        characters.put(TABLE_BOTTOM_INTERSECT,line.charAt(2)+"");
        characters.put(TABLE_BOTTOM_RIGHT,line.charAt(4)+"");
        return this;
    }
    /*
     * FBV      f      FBV
     */
    public AsciiTable footerLine(String line){
        characters.put(FOOTER_BORDER_LEFT,line.charAt(0)+"");
        characters.put(FOOTER_BORDER_HORIZONTAL,line.charAt(1)+"");
        return this;
    }
    /*
     * FBL FBH FBH FBH FBR
     */
    public AsciiTable footerBottom(String line){
        characters.put(FOOTER_BORDER_LEFT,line.charAt(0)+"");
        characters.put(FOOTER_BORDER_HORIZONTAL,line.charAt(1)+"");
        characters.put(FOOTER_BORDER_RIGHT,line.charAt(4)+"");
        return this;
    }
    public AsciiTable styleAll(String prefix, String suffix){
        this.prefixAll = prefix;
        this.suffixAll = suffix;
        return this;
    }
    public AsciiTable style(String key,String prefix, String suffix){
        prefixes.put(key,prefix);
        suffixes.put(key,suffix);
        return this;
    }
    public AsciiTable setCharacter(String key, String value){
        this.characters.put(key,value);
        return this;
    }
    public AsciiTable clearHeaderTopIntersect(int column){
        headerTopIntersect.remove(column);
        return this;
    }
    public AsciiTable setHeaderTopIntersect(int column,String value){
        headerTopIntersect.put(column,value);
        return this;
    }
    public AsciiTable clearHeaderColumnSeparator(int column){
        headerColumnSeparators.remove(column);
        return this;
    }
    public AsciiTable setHeaderColumnSeparator(int column,String separator){
        headerColumnSeparators.put(column,separator);
        return this;
    }
    public AsciiTable clearTableTopIntersect(int column){
        tableTopIntersect.remove(column);
        return this;
    }
    public AsciiTable setTableTopIntersect(int column,String value){
        tableTopIntersect.put(column,value);
        return this;
    }
    public AsciiTable clearTableColumnSeparator(int column){
        tableColumnSeparators.remove(column);
        return this;
    }
    public AsciiTable setTableColumnSeparator(int column,String separator){
        tableColumnSeparators.put(column,separator);
        return this;
    }
    public AsciiTable setRowSeparatorIntersect(int column,String separator){
        rowSeparatorIntersect.put(column,separator);
        return this;
    }
    public AsciiTable clearTableBottomIntersect(int column){
        tableBottomIntersect.remove(column);
        return this;
    }
    public AsciiTable setTableBottomIntersect(int column,String value){
        tableBottomIntersect.put(column,value);
        return this;
    }

    private String unwrap(Object obj){
        if(obj == null){
            return "--";
        }else if (obj instanceof Double || obj instanceof Float){
            return String.format("%."+precision+"f",obj);
        }else{
            return obj.toString();
        }
    }

    public <T> String render(int maxWidth,List<T> values,List<String> headers,List<Function<T,Object>> functions){
        StringBuilder rtrn = new StringBuilder();
        boolean outsideBorder = hasOutsideBorder();
        List<List<String>> headerRows = lineSplit(headers);
        int columnCount = Math.min(headers.size(),functions.size());
        List<List<String>> rows = new ArrayList<>();

        int[] columnWidths = headers.isEmpty() ?
                new int[columnCount] :
                headers.stream()
                        .mapToInt(header-> Stream.of(header.split(System.lineSeparator()))
                                .mapToInt(String::length).max().orElse(0)
                        )
                        .toArray();
        String[] columnFormats = Collections.nCopies(columnCount,"").toArray(new String[0]);
        tableColumnFormats.forEach((index,format)->{
            columnFormats[index] = format;
        });

        for(int vIndex=0; vIndex<columnCount; vIndex++){
            T value = values.get(vIndex);
            List<String> rowValues = new ArrayList<>();
            for(int fIndex=0; fIndex < functions.size(); fIndex++){
                Object cellValue =  functions.get(fIndex).apply(value);
                if( cellValue == null) {
                    cellValue = "";
                } else if (cellValue instanceof Long || cellValue instanceof Integer){
                    if(columnFormats[fIndex].isBlank()) {
                        columnFormats[fIndex] = "d";
                    }
                }else if (cellValue instanceof Double || cellValue instanceof Float){
                    if(columnFormats[fIndex].isBlank() || "d".equals(columnFormats[fIndex])){
                        columnFormats[fIndex] = "f";
                    }
                }else if (columnFormats[fIndex].isBlank()){
                    columnFormats[fIndex] = "s";
                }
                String cellString = unwrap(cellValue);
                rowValues.add(cellString);
            }
            List<List<String>> splitRow = lineSplit(rowValues);
            for(List<String> row : splitRow){
                for(int i=0; i<columnCount; i++){
                    int cellWidth = row.get(i).length();
                    if(cellWidth > columnWidths[i]){
                        columnWidths[i] = cellWidth;
                    }
                }
            }
            if(vIndex>0){
                rows.add(null);//indicates row divide
            }
            rows.addAll(splitRow);//add the split rows
        }
        for(int i=0; i<columnFormats.length; i++){
            if(columnFormats[i]==null){
                columnFormats[i] = "s";
            }
        }
        //look to truncate columns if needed
        int widthSum = IntStream.of(columnWidths).sum();
        if( widthSum > maxWidth - 3*(columnCount-1) - (outsideBorder ? 4 : 0) ){
            //TODO decide how to truncate widths
        }
        //TODO render caption
        //render headers
        if(!headers.isEmpty()){
            if(outsideBorder){
                rtrn.append(characters.get(HEADER_TOP_LEFT));
                rtrn.append(characters.get(HEADER_BORDER_HORIZONTAL));
                for(int c=0; c<columnCount; c++){
                    int width = columnWidths[c];
                    if(c>0){
                        rtrn.append(characters.get(HEADER_BORDER_HORIZONTAL));
                        if(this.headerTopIntersect.containsKey(c-1)){
                            rtrn.append(headerTopIntersect.get(c-1));
                        }else {
                            rtrn.append(characters.get(HEADER_TOP_INTERSECT));
                        }
                        rtrn.append(characters.get(HEADER_BORDER_HORIZONTAL));
                    }
                    for(int i=0; i<width; i++){
                        rtrn.append(characters.get(HEADER_BORDER_HORIZONTAL));
                    }
                }
                rtrn.append(characters.get(HEADER_BORDER_HORIZONTAL));
                rtrn.append(characters.get(HEADER_TOP_RIGHT));
                rtrn.append(System.lineSeparator());
            }
            for(int rowIndex=0; rowIndex<headerRows.size(); rowIndex++){
                List<String> headerRow = headerRows.get(rowIndex);
                if(outsideBorder){
                    rtrn.append(characters.get(HEADER_BORDER_LEFT));
                    rtrn.append(" ");
                }
                for(int c=0; c<columnCount; c++){
                    int width = columnWidths[c];
                    String header = headerRow.get(c);
                    int leftPad = (width-header.length())/2;
                    int remainder = width-leftPad-header.length();
                    if(c > 0){
                        rtrn.append(" ");
                        if(this.headerColumnSeparators.containsKey(c-1)){
                            rtrn.append(this.headerColumnSeparators.get(c-1));
                        } else {
                            rtrn.append(characters.get(HEADER_COLUMN_SEPARATOR));
                        }
                        rtrn.append(" ");
                    }
                    if(leftPad > 0){
                        rtrn.append(String.format("%"+leftPad+"s",""));
                    }
                    rtrn.append(header);
                    if(remainder > 0){
                        rtrn.append(String.format("%"+remainder+"s",""));
                    }
                }
                if(outsideBorder){
                    rtrn.append(" ");
                    rtrn.append(characters.get(HEADER_BORDER_RIGHT));
                }
                rtrn.append(System.lineSeparator());
            }
        }
        //render table top
        if(outsideBorder){
            rtrn.append(characters.get(TABLE_TOP_LEFT));
            rtrn.append(characters.get(TABLE_TOP_HORIZONTAL));
        }
        for(int c=0; c<columnCount; c++){
            int width = columnWidths[c];
            if(c>0){
                rtrn.append(characters.get(TABLE_TOP_HORIZONTAL));
                if(tableTopIntersect.containsKey(c-1)){
                    rtrn.append(tableTopIntersect.get(c-1));
                }else {
                    rtrn.append(characters.get(TABLE_TOP_INTERSECT));
                }
                rtrn.append(characters.get(TABLE_TOP_HORIZONTAL));
            }
            for(int i=0; i<width; i++){
                rtrn.append(characters.get(TABLE_TOP_HORIZONTAL));
            }
        }
        if(outsideBorder){
            rtrn.append(characters.get(TABLE_TOP_HORIZONTAL));
            rtrn.append(characters.get(TABLE_TOP_RIGHT));
        }
        rtrn.append(System.lineSeparator());
        //render table rows
        for(int rowIndex=0; rowIndex<rows.size(); rowIndex++){
            List<String> row = rows.get(rowIndex);
            if(row == null ){//row divider
                if(hasRowSeparator()){
                    if(outsideBorder) {
                        rtrn.append(characters.get(ROW_SEPARATOR_LEFT));
                        rtrn.append(characters.get(ROW_SEPARATOR_HORIZONTAL));
                    }
                    for(int c=0; c<columnCount; c++){
                        int width = columnWidths[c];
                        if(c>0 && hasRowSeparator()){
                            rtrn.append(characters.get(ROW_SEPARATOR_HORIZONTAL));
                            if(rowSeparatorIntersect.containsKey(c-1)){
                                rtrn.append(rowSeparatorIntersect.get(c-1));
                            } else {
                                rtrn.append(characters.get(ROW_SEPARATOR_INTERSECT));
                            }
                            rtrn.append(characters.get(ROW_SEPARATOR_HORIZONTAL));
                        }
                        for(int i=0; i<width; i++){
                            rtrn.append(characters.get(TABLE_TOP_HORIZONTAL));
                        }
                    }
                    if(outsideBorder) {
                        rtrn.append(characters.get(ROW_SEPARATOR_HORIZONTAL));
                        rtrn.append(characters.get(ROW_SEPARATOR_RIGHT));
                    }
                    rtrn.append(System.lineSeparator());
                }
            } else {//render the row
                //TBV  v  TCS  v  TBV    value (row)
                if(outsideBorder) {
                    rtrn.append(characters.get(TABLE_BORDER_LEFT));
                    rtrn.append(" ");
                }
                for(int c=0; c<columnCount; c++){
                    String cellString = row.get(c);
                    int width = columnWidths[c];
                    String format = columnFormats[c];
                    if(c>0){
                        rtrn.append(" ");
                        if(this.tableColumnSeparators.containsKey(c-1)){
                            rtrn.append(this.tableColumnSeparators.get(c-1));
                        } else {
                            rtrn.append(characters.get(TABLE_COLUMN_SEPARATOR));
                        }
                        rtrn.append(" ");
                    }
                    //todo use string.matches instead of tracking column format?
                    if(format.contains("f") || format.contains("d")) {
                        rtrn.append(String.format("%" + width + "s", cellString));
                    }else if (format.contains("c")) {
                        int leftPad = (width-cellString.length())/2;
                        int remainder = width-leftPad-cellString.length();
                        if(leftPad > 0){
                            rtrn.append(String.format("%"+leftPad+"s",""));
                        }
                        rtrn.append(cellString);
                        if(remainder > 0){
                            rtrn.append(String.format("%"+remainder+"s",""));
                        }
                    }else{
                        rtrn.append(String.format("%-"+width+"s",cellString));
                    }
                }
                if(outsideBorder) {
                    rtrn.append(" ");
                    rtrn.append(characters.get(TABLE_BORDER_RIGHT));

                }
                rtrn.append(System.lineSeparator());
            }
        }
        //render table bottom
        if(outsideBorder){
            rtrn.append(characters.get(TABLE_BOTTOM_LEFT));
            rtrn.append(characters.get(TABLE_BOTTOM_HORIZONTAL));
            for(int c=0; c<columnCount; c++){
                int width = columnWidths[c];
                if(c>0){
                    rtrn.append(characters.get(TABLE_BOTTOM_HORIZONTAL));
                    if(tableBottomIntersect.containsKey(c-1)){
                        rtrn.append(tableBottomIntersect.get(c-1));
                    }else {
                        rtrn.append(characters.get(TABLE_BOTTOM_INTERSECT));
                    }
                    rtrn.append(characters.get(TABLE_BOTTOM_HORIZONTAL));
                }
                for(int i=0; i<width; i++){
                    rtrn.append(characters.get(TABLE_BOTTOM_HORIZONTAL));
                }
            }
            rtrn.append(characters.get(TABLE_BOTTOM_HORIZONTAL));
            rtrn.append(characters.get(TABLE_BOTTOM_RIGHT));
        }
        //TODO render table footer
        return rtrn.toString();
    }

    public boolean hasRowSeparator(){
        return Stream.of(ROW_SEPARATOR_LEFT,ROW_SEPARATOR_HORIZONTAL,ROW_SEPARATOR_INTERSECT,ROW_SEPARATOR_RIGHT).allMatch(characters::containsKey);
    }
    public boolean hasOutsideBorder(){
        return Stream.of(HEADER_TOP_LEFT, HEADER_BORDER_HORIZONTAL,HEADER_TOP_INTERSECT, HEADER_TOP_RIGHT,
                HEADER_BORDER_LEFT,HEADER_BORDER_RIGHT, HEADER_COLUMN_SEPARATOR,
                TABLE_TOP_LEFT, TABLE_TOP_HORIZONTAL,TABLE_TOP_INTERSECT,TABLE_TOP_RIGHT,
                TABLE_BORDER_LEFT,TABLE_BORDER_RIGHT,TABLE_COLUMN_SEPARATOR,
                TABLE_BOTTOM_LEFT, TABLE_BOTTOM_HORIZONTAL, TABLE_BOTTOM_INTERSECT, TABLE_BOTTOM_RIGHT ).allMatch(characters::containsKey);
    }



}

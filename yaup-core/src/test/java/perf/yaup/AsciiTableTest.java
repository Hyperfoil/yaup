package perf.yaup;

import io.hyperfoil.tools.yaup.AsciiTable;
import org.junit.Test;

import java.util.List;

public class AsciiTableTest {


    @Test
    public void custom_template(){
        AsciiTable asciiTable = new AsciiTable(
           """
           ─────
            h h\s
           ─────
            v v\s
           ─────
           """
        ).setHeaderTopIntersect(0,"┬")
        .setHeaderColumnSeparator(0,"│")
                .setTableTopIntersect(0,"┼")
        .setTableColumnSeparator(0,"│")
        .setTableBottomIntersect(0,"┴")
                .setColumnFormat(1,"c")
                .setPrecision(3);
        ;

        System.out.println(asciiTable.render(80,
                List.of("uno","dos","tres","quatro"),List.of("one","two\nsecond","three truncated","length"),List.of(
                        (a)->a+"\n"+a.charAt(0),
                        (a)->""+a.charAt(1),
                        (a)->1.333333333333*a.length() < 5 ? 1.333333333333*a.length() : "toobig",
                        (a)->a.length() == 3 ? "nah" : a.length()
                )));
    }
}

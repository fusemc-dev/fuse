package dev;

import com.manchickas.jet.Jet;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

public class Main {

    static void main() {
        try (var ctx = Context.newBuilder("js")
                .allowHostAccess(HostAccess.newBuilder()
                        .allowAccessAnnotatedBy(HostAccess.Export.class)
                        .build())
                .build()) {
            var value = new Property("MyProp", Jet.undefined());
            ctx.getBindings("js")
               .putMember("prop", value);
            ctx.eval("js", """
                    console.log(prop(5))
                    console.log(prop)
                    console.log(prop())
                    """);
        }
    }
}

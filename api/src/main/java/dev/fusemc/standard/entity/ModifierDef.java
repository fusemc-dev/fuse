package dev.fusemc.standard.entity;

import com.manchickas.optionated.Option;
import dev.fusemc.standard.ProxyIdentifier;
import dev.fusemc.tau.Template;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.NotNull;

public record ModifierDef(@NotNull Identifier name,
                          @NotNull ModifierDef.Operation operation,
                          boolean temporary) {

    public static final Template<ModifierDef> TEMPLATE = Template.record(
            ProxyIdentifier.MAPPED_TEMPLATE
                    .property("name", ModifierDef::name),
            Operation.TEMPLATE
                    .property("operation", ModifierDef::operation),
            Template.BOOLEAN
                    .property("transient", ModifierDef::temporary)
                    .optional(() -> false),
            ModifierDef::new
    );

    public @NotNull AttributeModifier create() {
        return new AttributeModifier(this.name, this.operation.amount(), this.operation.type());
    }

    private record Operation(@NotNull AttributeModifier.Operation type, double amount) {

        private static final Template<Operation> TEMPLATE = Template.record(
                Template.union(
                        Template.literal("constant").flatMap(
                                _ -> Option.some(AttributeModifier.Operation.ADD_VALUE),
                                operation -> Option.fromNullable(operation)
                                        .filter(op -> op == AttributeModifier.Operation.ADD_VALUE)
                                        .map(_ -> "constant")
                        ),
                        Template.literal("multiplied_base").flatMap(
                                _ -> Option.some(AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                                operation -> Option.fromNullable(operation)
                                        .filter(op -> op == AttributeModifier.Operation.ADD_VALUE)
                                        .map(_ -> "multiplied_base")
                        ),
                        Template.literal("multiplied_total").flatMap(
                                _ -> Option.some(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                                operation -> Option.fromNullable(operation)
                                        .filter(op -> op == AttributeModifier.Operation.ADD_VALUE)
                                        .map(_ -> "multiplied_total")
                        )
                ).property("type", Operation::type),
                Template.DOUBLE.property("amount", Operation::amount),
                Operation::new
        );
    }
}

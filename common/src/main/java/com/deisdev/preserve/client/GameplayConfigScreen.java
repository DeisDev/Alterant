package com.deisdev.preserve.client;

import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.network.GameplayPayload;
import com.deisdev.preserve.network.GameplayRequest;
import com.deisdev.preserve.rules.ComponentTrade;
import com.deisdev.preserve.rules.GameplaySettingsFile;
import com.deisdev.preserve.rules.ServerPolicy;
import com.deisdev.preserve.rules.TimeSettings;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.gui.YACLScreen;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Only loaded through the optional config factory. Gameplay state always comes from the server. */
public final class GameplayConfigScreen {
    private GameplayConfigScreen() {}
    public static void open(Screen parent) {
        var session = new ClientGameplay.Session();
        if (!session.connected()) {
            Minecraft.getInstance().gui.setScreen(editor(parent, session, null, "offline"));
        } else { request(parent, session, 0, GameplayRequest.Action.READ, ""); }
    }
    private static void request(Screen parent, ClientGameplay.Session session, long revision, GameplayRequest.Action action, String json) {
        var client = Minecraft.getInstance();
        client.gui.setScreen(session.send(revision, action, json) ? new Pending(parent, session)
                : editor(parent, session, null, "unavailable"));
    }
    private static Component text(String key) { return Component.translatable("config.deisdev.gameplay." + key); }
    private static Screen editor(Screen parent, ClientGameplay.Session session, GameplayPayload reply, String message) {
        var policy = reply == null ? ServerPolicy.DEFAULT : GameplaySettingsFile.decode(reply.json());
        var draft = new Draft(policy, reply != null && reply.editable() && session.connected());
        var status = text(message != null ? message : !reply.editable() ? "readonly" : reply.overridden() ? "override" : "datapack");
        var builder = YetAnotherConfigLib.createBuilder().title(text("title"));
        var time = ConfigCategory.createBuilder().name(text("serums")).option(LabelOption.create(status))
                .option(LabelOption.create(text("future")));
        var defaults = TimeSettings.DEFAULT;
        time.option(draft.decimal("multiplier", defaults.multiplier(), () -> draft.multiplier, value -> draft.multiplier = value, 1.01, 8));
        time.option(draft.decimal("suspicious_min", defaults.suspiciousMin(), () -> draft.minimum, value -> draft.minimum = value, 1.01, 8));
        time.option(draft.decimal("suspicious_max", defaults.suspiciousMax(), () -> draft.maximum, value -> draft.maximum = value, 1.01, 8));
        time.option(draft.decimal("duration", 20, () -> draft.minutes, value -> draft.minutes = value, 1.0 / 1200, 1440));
        var enchanting = OptionGroup.createBuilder().name(text("enchanting"));
        enchanting.option(draft.integer("enchant_level", defaults.enchantLevel(), () -> draft.level, value -> draft.level = value, 1, 100));
        enchanting.option(draft.integer("enchant_levels_spent", defaults.enchantLevelsSpent(), () -> draft.spent, value -> draft.spent = value, 1, 100));
        enchanting.option(draft.integer("lapis_cost", defaults.lapisCost(), () -> draft.lapis, value -> draft.lapis = value, 1, 64));
        enchanting.option(draft.integer("bookshelves", defaults.bookshelves(), () -> draft.shelves, value -> draft.shelves = value, 0, 32));
        builder.category(time.group(enchanting.build()).build());
        var general = ConfigCategory.createBuilder().name(text("preservation")).option(LabelOption.create(status));
        general.option(draft.toggle("partial", true, () -> draft.partial, value -> draft.partial = value));
        general.option(draft.integer("chunk_limit", 4096, () -> draft.chunkLimit, value -> draft.chunkLimit = value, 1, 4096));
        general.option(draft.integer("area_limit", 9, () -> draft.areaLimit, value -> draft.areaLimit = value, 1, 9));
        for (var formulation : Formulation.values()) {
            general.option(draft.track("enabled." + formulation.getSerializedName(), Option.<Boolean>createBuilder()
                    .name(Component.translatable("item.deisdev." + formulation.getSerializedName()))
                    .description(OptionDescription.of(text("enabled.description"))).available(draft.editable)
                    .binding(true, () -> !draft.disabled.contains(formulation), value -> { if (value) { draft.disabled.remove(formulation); } else { draft.disabled.add(formulation); } })
                    .controller(TickBoxControllerBuilder::create).build()));
        }
        general.option(ButtonOption.createBuilder().name(text("reset")).text(text("reset.button"))
                .description(OptionDescription.of(text("reset.description"))).available(draft.editable)
                .action((screen, option) -> request(parent, session, reply.revision(), GameplayRequest.Action.RESET, "")).build());
        builder.category(general.build());
        var trades = ConfigCategory.createBuilder().name(text("trades")).option(LabelOption.create(status)).option(LabelOption.create(text("trades.description")));
        for (int index = 0; index < draft.trades.size(); index++) {
            var trade = draft.trades.get(index);
            String prefix = "trade." + index + ".";
            var group = OptionGroup.createBuilder().name(Component.translatable("item.deisdev." + trade.component)).collapsed(true);
            group.option(draft.toggle(prefix + "enabled", trade.defaultEnabled, () -> trade.enabled, value -> trade.enabled = value));
            group.option(draft.track(prefix + "profession", Option.<Profession>createBuilder().name(text("trade.profession"))
                    .available(draft.editable).binding(trade.defaultProfession, () -> trade.profession, value -> trade.profession = value)
                    .controller(option -> EnumControllerBuilder.create(option).enumClass(Profession.class)
                            .formatValue(value -> Component.translatable("entity.minecraft.villager." + value.name().toLowerCase(Locale.ROOT)))).build()));
            var base = trade.defaults;
            group.option(draft.integer(prefix + "level", base.level(), () -> trade.level, value -> trade.level = value, 1, 5));
            group.option(draft.integer(prefix + "emeralds", base.emeralds(), () -> trade.emeralds, value -> trade.emeralds = value, 1, 64));
            group.option(draft.integer(prefix + "count", base.count(), () -> trade.count, value -> trade.count = value, 1, 64));
            group.option(draft.integer(prefix + "max_uses", base.maxUses(), () -> trade.uses, value -> trade.uses = value, 1, 64));
            group.option(draft.integer(prefix + "end_stone", base.endStone(), () -> trade.endStone, value -> trade.endStone = value, 0, 64));
            trades.group(group.build());
        }
        builder.category(trades.build());
        builder.save(() -> request(parent, session, reply.revision(), GameplayRequest.Action.SAVE, GameplaySettingsFile.encode(draft.policy())));
        return new YACLScreen(builder.build(), parent) {
            @Override public void finishOrSave() {
                if (pendingChanges()) {
                    if (!draft.editable || !session.connected()) { saveButtonMessage = text("unavailable"); return; }
                    try { draft.pendingTime(); }
                    catch (IllegalArgumentException error) { saveButtonMessage = text("invalid"); return; }
                }
                super.finishOrSave();
            }
        };
    }
    private static final class Pending extends Screen {
        private final Screen parent;
        private final ClientGameplay.Session session;
        private int age;
        Pending(Screen parent, ClientGameplay.Session session) { super(text("loading")); this.parent = parent; this.session = session; }
        @Override protected void init() {
            addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose()).bounds(width / 2 - 75, height / 2 + 24, 150, 20).build());
        }
        @Override public void tick() {
            var reply = session.reply();
            if (reply != null) {
                String message = switch (reply.status()) {
                    case LOADED -> null;
                    default -> reply.status().name().toLowerCase(Locale.ROOT);
                };
                minecraft.gui.setScreen(editor(parent, session, reply, message));
            } else if (++age > 200 || !session.connected()) { minecraft.gui.setScreen(editor(parent, session, null, "unavailable")); }
        }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            graphics.text(font, title, (width - font.width(title)) / 2, height / 2 - 12, 0xFFFFFFFF);
        }
        @Override public void onClose() { minecraft.gui.setScreen(parent); }
    }
    private enum Profession { CLERIC, MASON, LEATHERWORKER, TOOLSMITH, ARMORER, WEAPONSMITH, LIBRARIAN, FARMER, FISHERMAN, SHEPHERD, FLETCHER, CARTOGRAPHER, BUTCHER }
    private static final class TradeDraft {
        final String component;
        final ComponentTrade defaults;
        final Profession defaultProfession;
        final boolean defaultEnabled;
        boolean enabled;
        Profession profession;
        int level, emeralds, count, uses, endStone;
        TradeDraft(ComponentTrade trade, boolean enabled) {
            component = trade.component();
            defaults = ComponentTrade.DEFAULT.stream().filter(value -> value.component().equals(component)).findFirst().orElse(trade);
            defaultProfession = Profession.valueOf(defaults.profession().toUpperCase(Locale.ROOT)); defaultEnabled = true;
            this.enabled = enabled; profession = Profession.valueOf(trade.profession().toUpperCase(Locale.ROOT));
            level = trade.level(); emeralds = trade.emeralds(); count = trade.count(); uses = trade.maxUses(); endStone = trade.endStone();
        }
        ComponentTrade build() { return new ComponentTrade(component, profession.name().toLowerCase(Locale.ROOT), level, emeralds, count, uses, endStone); }
    }
    private static final class Draft {
        final boolean editable;
        final Map<String, Option<?>> options = new LinkedHashMap<>();
        final EnumSet<Formulation> disabled;
        final List<TradeDraft> trades = new ArrayList<>();
        double multiplier, minimum, maximum, minutes;
        int level, spent, lapis, shelves, chunkLimit, areaLimit;
        boolean partial;
        Draft(ServerPolicy policy, boolean editable) {
            this.editable = editable;
            var time = policy.time(); multiplier = time.multiplier(); minimum = time.suspiciousMin(); maximum = time.suspiciousMax(); minutes = time.durationTicks() / 1200.0;
            level = time.enchantLevel(); spent = time.enchantLevelsSpent(); lapis = time.lapisCost(); shelves = time.bookshelves();
            disabled = EnumSet.noneOf(Formulation.class); disabled.addAll(policy.disabled());
            partial = policy.allowPartial(); chunkLimit = policy.chunkLimit(); areaLimit = policy.areaLimit();
            policy.componentTrades().forEach(trade -> trades.add(new TradeDraft(trade, true)));
            for (var trade : ComponentTrade.DEFAULT) {
                if (trades.size() < 32 && policy.componentTrades().stream().noneMatch(value -> value.component().equals(trade.component()))) { trades.add(new TradeDraft(trade, false)); }
            }
        }
        <T> Option<T> track(String key, Option<T> option) { options.put(key, option); return option; }
        String label(String key) { return key.startsWith("trade.") ? "trade." + key.substring(key.lastIndexOf('.') + 1) : key; }
        Option<Boolean> toggle(String key, boolean def, Supplier<Boolean> get, Consumer<Boolean> set) {
            return track(key, Option.<Boolean>createBuilder().name(text(label(key))).description(OptionDescription.of(text(label(key) + ".description")))
                    .available(editable).binding(def, get, set).controller(TickBoxControllerBuilder::create).build());
        }
        Option<Integer> integer(String key, int def, Supplier<Integer> get, Consumer<Integer> set, int min, int max) {
            return track(key, Option.<Integer>createBuilder().name(text(label(key))).description(OptionDescription.of(text(label(key) + ".description")))
                    .available(editable).binding(def, get, set).controller(option -> IntegerFieldControllerBuilder.create(option).range(min, max)).build());
        }
        Option<Double> decimal(String key, double def, Supplier<Double> get, Consumer<Double> set, double min, double max) {
            return track(key, Option.<Double>createBuilder().name(text(key)).description(OptionDescription.of(text(key + ".description")))
                    .available(editable).binding(def, get, set).controller(option -> DoubleFieldControllerBuilder.create(option).range(min, max)).build());
        }
        double number(String key) { return ((Number) options.get(key).pendingValue()).doubleValue(); }
        TimeSettings pendingTime() {
            return new TimeSettings(number("multiplier"), number("suspicious_min"), number("suspicious_max"), (int) Math.round(number("duration") * 1200),
                    (int) number("enchant_level"), (int) number("enchant_levels_spent"), (int) number("lapis_cost"), (int) number("bookshelves"));
        }
        ServerPolicy policy() {
            return new ServerPolicy(disabled, partial, chunkLimit, areaLimit,
                    new TimeSettings(multiplier, minimum, maximum, (int) Math.round(minutes * 1200), level, spent, lapis, shelves),
                    trades.stream().filter(trade -> trade.enabled).map(TradeDraft::build).toList());
        }
    }
}

package com.deisdev.alterant.client;

import com.deisdev.alterant.api.Formulation;
import com.deisdev.alterant.network.GameplayPayload;
import com.deisdev.alterant.network.GameplayRequest;
import com.deisdev.alterant.rules.ComponentTrade;
import com.deisdev.alterant.rules.GameplaySettingsFile;
import com.deisdev.alterant.rules.ServerPolicy;
import com.deisdev.alterant.rules.TimeSettings;
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
    private static Component text(String key) { return Component.translatable("config.alterant.gameplay." + key); }
    private static Screen editor(Screen parent, ClientGameplay.Session session, GameplayPayload reply, String message) {
        var policy = reply == null ? ServerPolicy.DEFAULT : GameplaySettingsFile.decode(reply.json());
        var draft = new Draft(policy, reply != null && reply.editable() && session.connected());
        var status = text(message != null ? message : !reply.editable() ? "readonly" : reply.overridden() ? "override" : "datapack");
        var builder = YetAnotherConfigLib.createBuilder().title(text("title"));
        var time = ConfigCategory.createBuilder().name(text("serums")).option(LabelOption.create(status))
                .option(LabelOption.create(text("future")));
        var defaults = TimeSettings.DEFAULT;
        for (int index = 0; index < Draft.PROFILES.size(); index++) {
            final int slot = index;
            String key = Draft.PROFILES.get(index);
            var base = defaults.profile(Draft.FORMULATIONS.get(index));
            var group = OptionGroup.createBuilder().name(Component.translatable("item.alterant." + Draft.FORMULATIONS.get(index).getSerializedName()));
            group.option(draft.decimal(key + ".multiplier", base.multiplier(), () -> draft.speeds[slot], value -> draft.speeds[slot] = value, 1.01, 8));
            group.option(draft.decimal(key + ".duration", base.durationTicks() / 1200.0, () -> draft.minutes[slot], value -> draft.minutes[slot] = value, 1.0 / 1200, 1440));
            time.group(group.build());
        }
        var suspicious = OptionGroup.createBuilder().name(Component.translatable("item.alterant.suspicious_time_serum"));
        suspicious.option(draft.decimal("suspicious.duration", 2, () -> draft.minutes[4], value -> draft.minutes[4] = value, 1.0 / 1200, 1440));
        suspicious.option(LabelOption.create(text("outcomes.description")));
        time.group(suspicious.build());
        for (int index = 0; index < 16; index++) {
            final int slot = index;
            String key = "outcome." + index;
            var base = index < TimeSettings.Suspicious.DEFAULT_OUTCOMES.size() ? TimeSettings.Suspicious.DEFAULT_OUTCOMES.get(index) : null;
            var group = OptionGroup.createBuilder().name(Component.translatable("config.alterant.gameplay.outcome", index + 1)).collapsed(true);
            group.option(draft.decimal(key + ".multiplier", base == null ? 2 : base.multiplier(), () -> draft.rollSpeeds[slot], value -> draft.rollSpeeds[slot] = value, 1.01, 8));
            group.option(draft.integer(key + ".weight", base == null ? 0 : base.weight(), () -> draft.weights[slot], value -> draft.weights[slot] = value, 0, 1000000));
            var chance = new ChanceState(() -> draft.chance(slot));
            draft.chances.add(chance);
            group.option(LabelOption.createBuilder().state(chance).build());
            time.group(group.build());
        }
        draft.refreshChances();
        draft.options.forEach((key, option) -> { if (key.startsWith("outcome.") && key.endsWith(".weight")) { option.addListener((changed, value) -> draft.refreshChances()); } });
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
                    .name(Component.translatable("item.alterant." + formulation.getSerializedName()))
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
            var group = OptionGroup.createBuilder().name(Component.translatable("item.alterant." + trade.component)).collapsed(true);
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
    /** Derived text emits updates to YACL widgets without becoming a pending gameplay edit. */
    private static final class ChanceState implements StateManager<Component> {
        private final Supplier<Component> calculate;
        private Component value;
        private StateListener<Component> listener = StateListener.noop();
        ChanceState(Supplier<Component> calculate) { this.calculate = calculate; value = calculate.get(); }
        @Override public Component get() { return value; }
        @Override public void sync() {
            var previous = value; value = calculate.get();
            if (!value.equals(previous)) { listener.onStateChange(previous, value); }
        }
        @Override public void set(Component ignored) {}
        @Override public void apply() {}
        @Override public void resetToDefault(ResetAction action) {}
        @Override public boolean isSynced() { return true; }
        @Override public boolean isAlwaysSynced() { return true; }
        @Override public boolean isDefault() { return true; }
        @Override public void addListener(StateListener<Component> next) { listener = listener.andThen(next); }
    }
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
        final List<ChanceState> chances = new ArrayList<>();
        static final List<String> PROFILES = List.of("regular", "refined", "enduring", "overcharged");
        static final List<Formulation> FORMULATIONS = List.of(Formulation.TIME_SERUM, Formulation.REFINED_TIME_SERUM, Formulation.ENDURING_TIME_SERUM, Formulation.OVERCHARGED_TIME_SERUM);
        final double[] speeds = new double[4], minutes = new double[5], rollSpeeds = new double[16];
        final int[] weights = new int[16];
        int level, spent, lapis, shelves, chunkLimit, areaLimit;
        boolean partial;
        Draft(ServerPolicy policy, boolean editable) {
            this.editable = editable;
            var time = policy.time();
            for (int index = 0; index < 4; index++) { var profile = time.profile(FORMULATIONS.get(index)); speeds[index] = profile.multiplier(); minutes[index] = profile.durationTicks() / 1200.0; }
            minutes[4] = time.suspicious().durationTicks() / 1200.0;
            var outcomes = time.suspicious().outcomes();
            for (int index = 0; index < 16; index++) { rollSpeeds[index] = index < outcomes.size() ? outcomes.get(index).multiplier() : 2; weights[index] = index < outcomes.size() ? outcomes.get(index).weight() : 0; }
            level = time.enchantLevel(); spent = time.enchantLevelsSpent(); lapis = time.lapisCost(); shelves = time.bookshelves();
            disabled = EnumSet.noneOf(Formulation.class); disabled.addAll(policy.disabled());
            partial = policy.allowPartial(); chunkLimit = policy.chunkLimit(); areaLimit = policy.areaLimit();
            policy.componentTrades().forEach(trade -> trades.add(new TradeDraft(trade, true)));
            for (var trade : ComponentTrade.DEFAULT) {
                if (trades.size() < 32 && policy.componentTrades().stream().noneMatch(value -> value.component().equals(trade.component()))) { trades.add(new TradeDraft(trade, false)); }
            }
        }
        <T> Option<T> track(String key, Option<T> option) { options.put(key, option); return option; }
        String label(String key) { return key.startsWith("trade.") ? "trade." + key.substring(key.lastIndexOf('.') + 1)
                    : key.startsWith("outcome.") ? "outcome." + key.substring(key.lastIndexOf('.') + 1) : key; }
        Option<Boolean> toggle(String key, boolean def, Supplier<Boolean> get, Consumer<Boolean> set) {
            return track(key, Option.<Boolean>createBuilder().name(text(label(key))).description(OptionDescription.of(text(label(key) + ".description")))
                    .available(editable).binding(def, get, set).controller(TickBoxControllerBuilder::create).build());
        }
        Option<Integer> integer(String key, int def, Supplier<Integer> get, Consumer<Integer> set, int min, int max) {
            return track(key, Option.<Integer>createBuilder().name(text(label(key))).description(OptionDescription.of(text(label(key) + ".description")))
                    .available(editable).binding(def, get, set).controller(option -> IntegerFieldControllerBuilder.create(option).range(min, max)).build());
        }
        Option<Double> decimal(String key, double def, Supplier<Double> get, Consumer<Double> set, double min, double max) {
            return track(key, Option.<Double>createBuilder().name(text(label(key))).description(OptionDescription.of(text(label(key) + ".description")))
                    .available(editable).binding(def, get, set).controller(option -> DoubleFieldControllerBuilder.create(option).range(min, max)).build());
        }
        double number(String key) { return ((Number) options.get(key).pendingValue()).doubleValue(); }
        void refreshChances() { chances.forEach(ChanceState::sync); }
        Component chance(int index) {
            int total = 0;
            for (int slot = 0; slot < 16; slot++) { var weight = options.get("outcome." + slot + ".weight"); if (weight != null) { total += ((Number) weight.pendingValue()).intValue(); } }
            var weight = options.get("outcome." + index + ".weight");
            if (total == 0 || weight == null) { return text("outcome.empty"); }
            return Component.translatable("config.alterant.gameplay.outcome.chance", String.format(Locale.ROOT, "%.2f", 100.0 * ((Number) weight.pendingValue()).intValue() / total));
        }
        TimeSettings.Profile pendingProfile(String key) {
            return new TimeSettings.Profile(number(key + ".multiplier"), (int) Math.round(number(key + ".duration") * 1200));
        }
        TimeSettings pendingTime() {
            var outcomes = new ArrayList<TimeSettings.Outcome>();
            for (int index = 0; index < 16; index++) { int weight = (int) number("outcome." + index + ".weight"); if (weight > 0) { outcomes.add(new TimeSettings.Outcome(number("outcome." + index + ".multiplier"), weight)); } }
            var suspicious = new TimeSettings.Suspicious((int) Math.round(number("suspicious.duration") * 1200), outcomes);
            return new TimeSettings(pendingProfile("regular"), suspicious, pendingProfile("refined"), pendingProfile("enduring"), pendingProfile("overcharged"),
                    (int) number("enchant_level"), (int) number("enchant_levels_spent"), (int) number("lapis_cost"), (int) number("bookshelves"));
        }
        ServerPolicy policy() {
            return new ServerPolicy(disabled, partial, chunkLimit, areaLimit,
                    pendingTime(),
                    trades.stream().filter(trade -> trade.enabled).map(TradeDraft::build).toList());
        }
    }
}

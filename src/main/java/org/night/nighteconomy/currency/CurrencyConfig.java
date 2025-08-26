package org.night.nighteconomy.currency;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CurrencyConfig {
    private String id;
    private String name;
    private double defaultValue;
    private boolean ranking;
    private int update;
    private String magnata;

    private FormatConfig format;

    private PaymentConfig payment;

    private CommandsConfig commands;

    private Map<String, String> messages;

    public CurrencyConfig() {}

    public CurrencyConfig(String id, String name, double defaultValue) {
        this.id = id;
        this.name = name;
        this.defaultValue = defaultValue;
        this.ranking = true;
        this.update = 300;
        this.magnata = "&a[$]";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getDefaultValue() { return defaultValue; }
    public void setDefaultValue(double defaultValue) { this.defaultValue = defaultValue; }

    public boolean isRanking() { return ranking; }
    public void setRanking(boolean ranking) { this.ranking = ranking; }

    public int getUpdate() { return update; }
    public void setUpdate(int update) { this.update = update; }

    public String getMagnata() { return magnata; }
    public void setMagnata(String magnata) { this.magnata = magnata; }

    public FormatConfig getFormat() { return format; }
    public void setFormat(FormatConfig format) { this.format = format; }

    public PaymentConfig getPayment() { return payment; }
    public void setPayment(PaymentConfig payment) { this.payment = payment; }

    public CommandsConfig getCommands() { return commands; }
    public void setCommands(CommandsConfig commands) { this.commands = commands; }

    public Map<String, String> getMessages() { return messages; }
    public void setMessages(Map<String, String> messages) { this.messages = messages; }

    public static class FormatConfig {
        private String format;
        private SeparatorConfig separator;
        private MultiplesConfig multiples;

        private Boolean centsEnabled;
        private String decimalSeparator;

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public SeparatorConfig getSeparator() { return separator; }
        public void setSeparator(SeparatorConfig separator) { this.separator = separator; }

        public MultiplesConfig getMultiples() { return multiples; }
        public void setMultiples(MultiplesConfig multiples) { this.multiples = multiples; }

        public Boolean getCentsEnabled() { return centsEnabled; }
        public void setCentsEnabled(Boolean centsEnabled) { this.centsEnabled = centsEnabled; }

        public String getDecimalSeparator() { return decimalSeparator; }
        public void setDecimalSeparator(String decimalSeparator) { this.decimalSeparator = decimalSeparator; }
    }

    public static class SeparatorConfig {
        private String decimal;
        private String group;
        private String single;

        public String getDecimal() { return decimal; }
        public void setDecimal(String decimal) { this.decimal = decimal; }

        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }

        public String getSingle() { return single; }
        public void setSingle(String single) { this.single = single; }
    }

    public static class MultiplesConfig {
        private boolean enabled;
        private int start;
        private List<String> multiples;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getStart() { return start; }
        public void setStart(int start) { this.start = start; }

        public List<String> getMultiples() { return multiples; }
        public void setMultiples(List<String> multiples) { this.multiples = multiples; }
    }

    public static class PaymentConfig {
        private boolean toggle;
        private double fee;

        public boolean isToggle() { return toggle; }
        public void setToggle(boolean toggle) { this.toggle = toggle; }

        public double getFee() { return fee; }
        public void setFee(double fee) { this.fee = fee; }
    }

    public static class CommandsConfig {
        private List<String> main;
        private String permission;
        private SubcommandsConfig subcommands;

        public List<String> getMain() { return main; }
        public void setMain(List<String> main) { this.main = main; }

        public String getPermission() { return permission; }
        public void setPermission(String permission) { this.permission = permission; }

        public SubcommandsConfig getSubcommands() { return subcommands; }
        public void setSubcommands(SubcommandsConfig subcommands) { this.subcommands = subcommands; }
    }

    public static class SubcommandsConfig {
        private SubcommandConfig see;
        private SubcommandConfig pay;
        private SubcommandConfig add;
        private TransactionsConfig transactions;
        private SubcommandConfig remove;
        private SubcommandConfig set;
        private SubcommandConfig top;
        private SubcommandConfig reset;
        private SubcommandConfig reload;
        private SubcommandConfig togglePayment;

        public SubcommandConfig getSee() { return see; }
        public void setSee(SubcommandConfig see) { this.see = see; }

        public SubcommandConfig getPay() { return pay; }
        public void setPay(SubcommandConfig pay) { this.pay = pay; }

        public SubcommandConfig getAdd() { return add; }
        public void setAdd(SubcommandConfig add) { this.add = add; }

        public TransactionsConfig getTransactions() { return transactions; }
        public void setTransactions(TransactionsConfig transactions) { this.transactions = transactions; }

        public SubcommandConfig getRemove() { return remove; }
        public void setRemove(SubcommandConfig remove) { this.remove = remove; }

        public SubcommandConfig getSet() { return set; }
        public void setSet(SubcommandConfig set) { this.set = set; }

        public SubcommandConfig getTop() { return top; }
        public void setTop(SubcommandConfig top) { this.top = top; }

        public SubcommandConfig getReset() { return reset; }
        public void setReset(SubcommandConfig reset) { this.reset = reset; }

        public SubcommandConfig getReload() { return reload; }
        public void setReload(SubcommandConfig reload) { this.reload = reload; }

        public SubcommandConfig getTogglePayment() { return togglePayment; }
        public void setTogglePayment(SubcommandConfig togglePayment) { this.togglePayment = togglePayment; }
    }

    public static class SubcommandConfig {
        private List<String> aliases;
        private String permission;
        private List<String> permissions;

        public List<String> getAliases() { return aliases; }
        public void setAliases(List<String> aliases) { this.aliases = aliases; }

        public String getPermission() { return permission; }
        public void setPermission(String permission) { this.permission = permission; }

        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    }

    public static class TransactionsConfig extends SubcommandConfig {
        private TransactionTypesConfig types;

        public TransactionTypesConfig getTypes() { return types; }
        public void setTypes(TransactionTypesConfig types) { this.types = types; }
    }

    public static class TransactionTypesConfig {
        private String all;
        private String add;
        private String remove;
        private String set;
        private String reset;
        private String paySend;
        private String payReceive;
        private String externalAdd;
        private String externalRemove;

        public String getAll() { return all; }
        public void setAll(String all) { this.all = all; }

        public String getAdd() { return add; }
        public void setAdd(String add) { this.add = add; }

        public String getRemove() { return remove; }
        public void setRemove(String remove) { this.remove = remove; }

        public String getSet() { return set; }
        public void setSet(String set) { this.set = set; }

        public String getReset() { return reset; }
        public void setReset(String reset) { this.reset = reset; }

        public String getPaySend() { return paySend; }
        public void setPaySend(String paySend) { this.paySend = paySend; }

        public String getPayReceive() { return payReceive; }
        public void setPayReceive(String payReceive) { this.payReceive = payReceive; }

        public String getExternalAdd() { return externalAdd; }
        public void setExternalAdd(String externalAdd) { this.externalAdd = externalAdd; }

        public String getExternalRemove() { return externalRemove; }
        public void setExternalRemove(String externalRemove) { this.externalRemove = externalRemove; }
    }

    public static CurrencyConfig createDefault(String id, String name) {
        CurrencyConfig c = new CurrencyConfig(id, name, 100.0);

        FormatConfig fmt = new FormatConfig();
        fmt.setCentsEnabled(true);
        fmt.setDecimalSeparator(",");
        SeparatorConfig sep = new SeparatorConfig();
        sep.setDecimal(",");
        sep.setGroup("");
        sep.setSingle("");
        fmt.setSeparator(sep);
        MultiplesConfig mul = new MultiplesConfig();
        mul.setEnabled(false);
        mul.setStart(0);
        mul.setMultiples(java.util.List.of());
        fmt.setMultiples(mul);
        c.setFormat(fmt);

        PaymentConfig pay = new PaymentConfig();
        pay.setToggle(true);
        pay.setFee(0.0);
        c.setPayment(pay);

        CommandsConfig cmds = new CommandsConfig();
        cmds.setMain(java.util.List.of(id));
        cmds.setPermission("nighteconomy." + id + ".use");

        SubcommandsConfig subs = new SubcommandsConfig();

        java.util.function.Function<String, SubcommandConfig> mk = (perm) -> {
            SubcommandConfig sc = new SubcommandConfig();
            sc.setAliases(java.util.List.of());
            sc.setPermission(perm);
            sc.setPermissions(java.util.List.of());
            return sc;
        };

        subs.setSee(mk.apply("nighteconomy." + id + ".see"));
        subs.setPay(mk.apply("nighteconomy." + id + ".pay"));
        subs.setAdd(mk.apply("nighteconomy." + id + ".add"));
        subs.setRemove(mk.apply("nighteconomy." + id + ".remove"));
        subs.setSet(mk.apply("nighteconomy." + id + ".set"));
        subs.setTop(mk.apply("nighteconomy." + id + ".top"));
        subs.setReset(mk.apply("nighteconomy." + id + ".reset"));
        subs.setReload(mk.apply("nighteconomy." + id + ".reload"));
        subs.setTogglePayment(mk.apply("nighteconomy." + id + ".toggle"));

        TransactionsConfig tx = new TransactionsConfig();
        tx.setAliases(java.util.List.of());
        tx.setPermission("nighteconomy." + id + ".transactions");
        tx.setPermissions(java.util.List.of("nighteconomy." + id + ".transactions.other"));

        TransactionTypesConfig types = new TransactionTypesConfig();
        types.setAll("ALL");
        types.setAdd("ADD");
        types.setRemove("REMOVE");
        types.setSet("SET");
        types.setReset("RESET");
        types.setPaySend("PAY_SEND");
        types.setPayReceive("PAY_RECEIVE");
        types.setExternalAdd("EXTERNAL_ADD");
        types.setExternalRemove("EXTERNAL_REMOVE");
        tx.setTypes(types);

        subs.setTransactions(tx);
        cmds.setSubcommands(subs);
        c.setCommands(cmds);

        Map<String, String> msgs = new HashMap<>();
        msgs.put("balance", "&aYour current balance: &f{amount}");
        msgs.put("balance-other", "&aBalance of {player}: &f{amount}");
        msgs.put("pay-sent", "&aYou paid &f{amount} &afor &f{player}!");
        msgs.put("pay-received", "&aYou received &f{amount} &afrom &f{player}!");
        msgs.put("insufficient-funds", "&cYou do not have enough balance!");
        msgs.put("invalid-amount", "&cInvalid amount!");
        c.setMessages(msgs);

        return c;
    }

    public static CurrencyConfig fromConfig(UnmodifiableConfig cfg) {
        if (cfg == null) return null;

        String id = cfg.getOrElse("id", "");
        String name = cfg.getOrElse("name", id);
        double def = cfg.getOrElse("defaultValue", 0.0);

        if (id == null || id.isEmpty()) return null;

        CurrencyConfig c = new CurrencyConfig(id, name, def);
        c.setRanking(cfg.getOrElse("ranking", true));
        c.setUpdate(cfg.getOrElse("update", 300));
        c.setMagnata(cfg.getOrElse("magnata", "&a[$]"));

        if (cfg.contains("format")) {
            UnmodifiableConfig f = cfg.get("format");
            FormatConfig fmt = new FormatConfig();

            Object fval = f.get("format");
            if (fval instanceof Boolean b) {
                fmt.setCentsEnabled(b);
                fmt.setFormat(null);
            } else if (fval instanceof String s) {
                fmt.setFormat(s);
                if ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
                    fmt.setCentsEnabled(Boolean.parseBoolean(s));
                }
            } else {
                fmt.setCentsEnabled(true);
                fmt.setFormat(null);
            }

            if (f.contains("separator")) {
                Object sepNode = f.get("separator");
                if (sepNode instanceof String ds) {
                    fmt.setDecimalSeparator(ds != null && !ds.isEmpty() ? ds : ",");
                    SeparatorConfig sep = new SeparatorConfig();
                    sep.setDecimal(fmt.getDecimalSeparator());
                    sep.setGroup("");
                    sep.setSingle("");
                    fmt.setSeparator(sep);
                } else if (sepNode instanceof UnmodifiableConfig s) {
                    SeparatorConfig sep = new SeparatorConfig();
                    String dec = s.getOrElse("decimal", ",");
                    sep.setDecimal(dec);
                    sep.setGroup(s.getOrElse("group", ""));
                    sep.setSingle(s.getOrElse("single", ""));
                    fmt.setSeparator(sep);
                    fmt.setDecimalSeparator(dec);
                }
            } else {
                fmt.setDecimalSeparator(",");
                SeparatorConfig sep = new SeparatorConfig();
                sep.setDecimal(",");
                sep.setGroup("");
                sep.setSingle("");
                fmt.setSeparator(sep);
            }

            if (f.contains("multiples")) {
                UnmodifiableConfig m = f.get("multiples");
                MultiplesConfig mul = new MultiplesConfig();
                mul.setEnabled(m.getOrElse("enabled", false));
                mul.setStart(m.getOrElse("start", 0));
                List<String> list = m.get("multiples");
                mul.setMultiples(list != null ? list : List.of());
                fmt.setMultiples(mul);
            } else {
                MultiplesConfig mul = new MultiplesConfig();
                mul.setEnabled(false);
                mul.setStart(0);
                mul.setMultiples(List.of());
                fmt.setMultiples(mul);
            }

            c.setFormat(fmt);
        }

        if (cfg.contains("payment")) {
            UnmodifiableConfig p = cfg.get("payment");
            PaymentConfig pay = new PaymentConfig();
            pay.setToggle(p.getOrElse("toggle", true));
            pay.setFee(p.getOrElse("fee", 0.0));
            c.setPayment(pay);
        }

        if (cfg.contains("commands")) {
            UnmodifiableConfig cm = cfg.get("commands");
            CommandsConfig cmds = new CommandsConfig();
            cmds.setMain(cm.getOrElse("main", List.of(id)));
            cmds.setPermission(cm.getOrElse("permission", "nighteconomy." + id + ".use"));

            if (cm.contains("subcommands")) {
                UnmodifiableConfig sc = cm.get("subcommands");
                SubcommandsConfig subs = new SubcommandsConfig();

                java.util.function.Function<String, SubcommandConfig> readSub = key -> {
                    if (!sc.contains(key)) return null;
                    UnmodifiableConfig sub = sc.get(key);
                    SubcommandConfig out = new SubcommandConfig();
                    out.setAliases(sub.getOrElse("aliases", List.of()));
                    out.setPermission(sub.getOrElse("permission", ""));
                    out.setPermissions(sub.getOrElse("permissions", List.of()));
                    return out;
                };

                subs.setSee(readSub.apply("see"));
                subs.setPay(readSub.apply("pay"));
                subs.setAdd(readSub.apply("add"));
                subs.setRemove(readSub.apply("remove"));
                subs.setSet(readSub.apply("set"));
                subs.setTop(readSub.apply("top"));
                subs.setReset(readSub.apply("reset"));
                subs.setReload(readSub.apply("reload"));
                subs.setTogglePayment(readSub.apply("togglePayment"));

                if (sc.contains("transactions")) {
                    UnmodifiableConfig tr = sc.get("transactions");
                    TransactionsConfig tcfg = new TransactionsConfig();
                    tcfg.setAliases(tr.getOrElse("aliases", List.of()));
                    tcfg.setPermission(tr.getOrElse("permission", ""));
                    tcfg.setPermissions(tr.getOrElse("permissions", List.of()));

                    if (tr.contains("types")) {
                        UnmodifiableConfig ty = tr.get("types");
                        TransactionTypesConfig types = new TransactionTypesConfig();
                        types.setAll(ty.getOrElse("all", "ALL"));
                        types.setAdd(ty.getOrElse("add", "ADD"));
                        types.setRemove(ty.getOrElse("remove", "REMOVE"));
                        types.setSet(ty.getOrElse("set", "SET"));
                        types.setReset(ty.getOrElse("reset", "RESET"));
                        types.setPaySend(ty.getOrElse("paySend", "PAY_SEND"));
                        types.setPayReceive(ty.getOrElse("payReceive", "PAY_RECEIVE"));
                        types.setExternalAdd(ty.getOrElse("externalAdd", "EXTERNAL_ADD"));
                        types.setExternalRemove(ty.getOrElse("externalRemove", "externalRemove"));
                        tcfg.setTypes(types);
                    }
                    subs.setTransactions(tcfg);
                }

                cmds.setSubcommands(subs);
            }

            c.setCommands(cmds);
        }

        Map<String, String> msgs = new HashMap<>();
        if (cfg.contains("messages")) {
            Object messagesObj = cfg.get("messages");
            if (messagesObj instanceof UnmodifiableConfig uc) {
                if (uc.contains("balance")) {
                    Object bal = uc.get("balance");
                    if (bal instanceof UnmodifiableConfig bc) {
                        String self = bc.get("self");
                        String other = bc.get("other");
                        if (self != null) msgs.put("balance", self);
                        if (other != null) msgs.put("balance-other", other);
                    }
                }
                if (uc.contains("pay")) {
                    Object pay = uc.get("pay");
                    if (pay instanceof UnmodifiableConfig pc) {
                        String sent = pc.get("sent");
                        String received = pc.get("received");
                        if (sent != null) msgs.put("pay-sent", sent);
                        if (received != null) msgs.put("pay-received", received);
                    }
                }
                flattenMessages("", uc, msgs);
            } else if (messagesObj instanceof Map<?, ?> raw) {
                for (Map.Entry<?, ?> e : raw.entrySet()) {
                    Object k = e.getKey();
                    Object v = e.getValue();
                    if (k != null && v != null) {
                        msgs.put(String.valueOf(k), String.valueOf(v));
                    }
                }
            }
        }
        c.setMessages(msgs);

        return c;
    }

    public void saveToFile(Path file) {
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(file)
                .preserveInsertionOrder()
                .sync()
                .writingMode(WritingMode.REPLACE)
                .build()) {

            CommentedConfig root = cfg;

            root.set("id", getId());
            root.setComment("id", "Currency ID. No spaces. Used as an internal key and in commands (e.g.: /" + getId() + ").");

            root.set("name", getName());
            root.setComment("name", "Visible currency name. Appears in messages and placeholders.");

            root.set("defaultValue", getDefaultValue());
            root.setComment("defaultValue", "Starting value of all players in this currency.");

            root.set("ranking", isRanking());
            root.setComment("ranking", "Enables (true) or disables (false) the ranking system for this currency.");

            root.set("update", getUpdate());
            root.setComment("update", "Interval (seconds) to refresh the ranking cache when ranking=true.");

            root.set("magnata", getMagnata());
            root.setComment("magnata", "'Tycoon' text/icon (top 1). Supports colors using &.");

            if (getFormat() != null) {
                CommentedConfig f = CommentedConfig.inMemory();
                root.set("format", f);
                root.setComment("format", "Display formatting of values for this currency.");

                boolean cents = getFormat().getCentsEnabled() != null ? getFormat().getCentsEnabled() : true;
                String sep = getFormat().getDecimalSeparator() != null && !getFormat().getDecimalSeparator().isEmpty()
                        ? getFormat().getDecimalSeparator() : ",";

                f.set("format", cents);
                f.setComment("format",
                        "Value model. Leave it \"true\" to display the currency's cents, or \"false\" to not display it.\n" +
                                "Example of true: Your money: 1000.0; example of false: Your money: 1000");

                f.set("separator", sep);
                f.setComment("separator", "If format is on, the \"separator\" will indicate how the cents will be separated.");
            }

            if (getPayment() != null) {
                CommentedConfig p = CommentedConfig.inMemory();
                root.set("payment", p);
                root.setComment("payment", "Payments between players.");

                p.set("toggle", getPayment().isToggle());
                p.setComment("toggle", "Allows the player to enable/disable receiving payments.");

                p.set("fee", getPayment().getFee());
                p.setComment("fee", "Fee charged on payments (absolute amount). Use 0.0 for no fee..");
            }

            if (getCommands() != null) {
                CommentedConfig cm = CommentedConfig.inMemory();
                root.set("commands", cm);
                root.setComment("commands", "Command configuration for this currency.");

                cm.set("main", getCommands().getMain());
                cm.setComment("main", "List of main commands/aliases that point to this coin (example.: [\"" + getId() + "\"]).");

                cm.set("permission", getCommands().getPermission());
                cm.setComment("permission", "Base permission to use commands for this currency.");

                if (getCommands().getSubcommands() != null) {
                    CommentedConfig sc = CommentedConfig.inMemory();
                    cm.set("subcommands", sc);
                    cm.setComment("subcommands", "Permissions and aliases by subcommand.");

                    java.util.function.BiConsumer<String, SubcommandConfig> putSub = (key, sub) -> {
                        if (sub == null) return;
                        CommentedConfig s = CommentedConfig.inMemory();
                        sc.set(key, s);
                        sc.setComment(key, switch (key) {
                            case "see" -> "View another player's balance: /<cmd> <player>";
                            case "pay" -> "Pay another player: /<cmd> pay <player> <amount>";
                            case "add" -> "Add balance to a player: /<cmd> add <player> <amount>";
                            case "remove" -> "Remove balance from a player: /<cmd> remove <player> <amount>";
                            case "set" -> "Set a player's balance: /<cmd> set <player> <amount>";
                            case "top" -> "View ranking: /<cmd> top";
                            case "reset" -> "Reset a player's balance: /<cmd> reset <player>";
                            case "reload" -> "Reload this currency's configuration: /<cmd> reload";
                            case "togglePayment" -> "Enable/disable receiving payments: /<cmd> toggle";
                            case "transactions" -> "List transactions: /<cmd> transactions [player]";
                            default -> "Subcomand " + key;
                        });

                        if (sub.getAliases() != null) {
                            s.set("aliases", sub.getAliases());
                            s.setComment("aliases", "Additional aliases for this subcommand.");
                        }
                        if (sub.getPermission() != null) {
                            s.set("permission", sub.getPermission());
                            s.setComment("permission", "Permission required to run this subcommand. Leave empty to not require permission..");
                        }
                        if (sub.getPermissions() != null) {
                            s.set("permissions", sub.getPermissions());
                            s.setComment("permissions", "Extra permissions (e.g. '...transactions.other' to view other player's transactions).");
                        }
                    };

                    putSub.accept("see", getCommands().getSubcommands().getSee());
                    putSub.accept("pay", getCommands().getSubcommands().getPay());
                    putSub.accept("add", getCommands().getSubcommands().getAdd());
                    putSub.accept("remove", getCommands().getSubcommands().getRemove());
                    putSub.accept("set", getCommands().getSubcommands().getSet());
                    putSub.accept("top", getCommands().getSubcommands().getTop());
                    putSub.accept("reset", getCommands().getSubcommands().getReset());
                    putSub.accept("reload", getCommands().getSubcommands().getReload());
                    putSub.accept("togglePayment", getCommands().getSubcommands().getTogglePayment());

                    if (getCommands().getSubcommands().getTransactions() != null) {
                        TransactionsConfig tr = getCommands().getSubcommands().getTransactions();
                        CommentedConfig tcfg = CommentedConfig.inMemory();
                        sc.set("transactions", tcfg);
                        sc.setComment("transactions", "Subcommand to list transactions. 'permissions' controls viewing of other players.");

                        if (tr.getAliases() != null) {
                            tcfg.set("aliases", tr.getAliases());
                            tcfg.setComment("aliases", "Additional aliases for 'transactions''.");
                        }
                        if (tr.getPermission() != null) {
                            tcfg.set("permission", tr.getPermission());
                            tcfg.setComment("permission", "Permission to use /<cmd> transactions.");
                        }
                        if (tr.getPermissions() != null) {
                            tcfg.set("permissions", tr.getPermissions());
                            tcfg.setComment("permissions", "Extra permissions (e.g. '...transactions.other' to view others).");
                        }

                        if (tr.getTypes() != null) {
                            CommentedConfig ty = CommentedConfig.inMemory();
                            tcfg.set("types", ty);
                            tcfg.setComment("types", "Transaction type labels (display only).");

                            ty.set("all", tr.getTypes().getAll());
                            ty.setComment("all", "Label: All transactions.");
                            ty.set("add", tr.getTypes().getAdd());
                            ty.setComment("add", "Label: Balance addition.");
                            ty.set("remove", tr.getTypes().getRemove());
                            ty.setComment("remove", "Label: Balance Removal.");
                            ty.set("set", tr.getTypes().getSet());
                            ty.setComment("set", "Label: Direct balance definition.");
                            ty.set("reset", tr.getTypes().getReset());
                            ty.setComment("reset", "Label: Balance Reset.");
                            ty.set("paySend", tr.getTypes().getPaySend());
                            ty.setComment("paySend", "Label: Payment sent.");
                            ty.set("payReceive", tr.getTypes().getPayReceive());
                            ty.setComment("payReceive", "Label: Payment received.");
                            ty.set("externalAdd", tr.getTypes().getExternalAdd());
                            ty.setComment("externalAdd", "Label: External credit (integration).");
                            ty.set("externalRemove", tr.getTypes().getExternalRemove());
                            ty.setComment("externalRemove", "Label: External debit (integration).");
                        }
                    }
                }
            }

            if (getMessages() != null) {
                CommentedConfig msg = CommentedConfig.inMemory();
                root.set("messages", msg);
                root.setComment("messages", "Messages for this currency. Placeholders: {amount}, {player}. Use and for colors.");

                putMessageWithComment(msg, "balance",
                        getMessages().get("balance"),
                        "When using /<cmd>: message from the player himself (current balance).");

                putMessageWithComment(msg, "balance-other",
                        getMessages().get("balance-other"),
                        "When using /<cmd> <player>: other player's balance. Placeholders: {player}, {amount}");

                putMessageWithComment(msg, "pay-sent",
                        getMessages().get("pay-sent"),
                        "When sending a payment. Placeholders: {player}, {amount}");

                putMessageWithComment(msg, "pay-received",
                        getMessages().get("pay-received"),
                        "When receiving a payment. Placeholders: {player}, {amount}");

                putMessageWithComment(msg, "insufficient-funds",
                        getMessages().get("insufficient-funds"),
                        "Error: insufficient balance for the operation.");

                putMessageWithComment(msg, "invalid-amount",
                        getMessages().get("invalid-amount"),
                        "Error: Invalid amount.");

                for (Map.Entry<String, String> e : this.messages.entrySet()) {
                    String k = e.getKey();
                    if (!msg.contains(k)) {
                        msg.set(k, e.getValue());
                    }
                }
            }

            cfg.save();
        }
    }

    private static void putMessageWithComment(CommentedConfig msg, String key, String value, String comment) {
        if (value != null) {
            msg.set(key, value);
            msg.setComment(key, comment);
        }
    }

    private static void flattenMessages(String prefix, UnmodifiableConfig uc, Map<String, String> out) {
        for (Map.Entry<String, Object> e : uc.valueMap().entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            String full = prefix.isEmpty() ? key : prefix + "-" + key;
            if (val instanceof UnmodifiableConfig nested) {
                flattenMessages(full, nested, out);
            } else if (val != null) {
                out.put(full, String.valueOf(val));
            }
        }
    }

    private static com.electronwill.nightconfig.core.CommentedConfig toNode(java.util.Map<String, ?> map) {
        var node = com.electronwill.nightconfig.core.CommentedConfig.inMemory();
        for (var e : map.entrySet()) {
            Object v = e.getValue();
            if (v instanceof java.util.Map<?, ?> m) {
                node.set(e.getKey(), toNode((java.util.Map<String, ?>) m));
            } else {
                node.set(e.getKey(), v);
            }
        }
        return node;
    }
}
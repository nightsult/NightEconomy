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
    
    // Format configuration
    private FormatConfig format;
    
    // Payment configuration
    private PaymentConfig payment;
    
    // Commands configuration
    private CommandsConfig commands;
    
    // Messages configuration
    private Map<String, String> messages;
    
    // Constructors
    public CurrencyConfig() {}
    
    public CurrencyConfig(String id, String name, double defaultValue) {
        this.id = id;
        this.name = name;
        this.defaultValue = defaultValue;
        this.ranking = true;
        this.update = 300;
        this.magnata = "&a[$]";
    }

    // Getters and Setters
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
    
    // Inner classes for nested configurations
    public static class FormatConfig {
        private String format;
        private SeparatorConfig separator;
        private MultiplesConfig multiples;
        
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        public SeparatorConfig getSeparator() { return separator; }
        public void setSeparator(SeparatorConfig separator) { this.separator = separator; }
        
        public MultiplesConfig getMultiples() { return multiples; }
        public void setMultiples(MultiplesConfig multiples) { this.multiples = multiples; }
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
        
        // Getters and setters for all subcommands
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
        
        // Getters and setters
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
        CurrencyConfig c = new CurrencyConfig(id, name, 0.0);

        // Format
        FormatConfig fmt = new FormatConfig();
        fmt.setFormat("&a$amount");
        SeparatorConfig sep = new SeparatorConfig();
        sep.setDecimal(",");
        sep.setGroup(".");
        sep.setSingle(" ");
        fmt.setSeparator(sep);
        MultiplesConfig mul = new MultiplesConfig();
        mul.setEnabled(true);
        mul.setStart(1000);
        mul.setMultiples(java.util.List.of("K","M","B","T"));
        fmt.setMultiples(mul);
        c.setFormat(fmt);

        // Payment
        PaymentConfig pay = new PaymentConfig();
        pay.setToggle(true);
        pay.setFee(0.0);
        c.setPayment(pay);

        // Commands
        CommandsConfig cmds = new CommandsConfig();
        cmds.setMain(java.util.List.of(id)); // alias principal igual ao id
        cmds.setPermission("nighteconomy." + id + ".use");
        SubcommandsConfig subs = new SubcommandsConfig();
        // permissões default vazias; pode preencher conforme necessidade
        cmds.setSubcommands(subs);
        c.setCommands(cmds);

        // Messages
        java.util.Map<String,String> msgs = new HashMap<>();
        msgs.put("balance.self", "&aSeu saldo: &f$amount");
        msgs.put("balance.other", "&aSaldo de &f$player&a: &f$amount");
        msgs.put("pay.sent", "&aVocê pagou &f$amount &apara &f$player&a.");
        msgs.put("pay.received", "&aVocê recebeu &f$amount &ade &f$player&a.");
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

        // format
        if (cfg.contains("format")) {
            UnmodifiableConfig f = cfg.get("format");
            FormatConfig fmt = new FormatConfig();
            fmt.setFormat(f.getOrElse("format", "&a$amount"));

            if (f.contains("separator")) {
                UnmodifiableConfig s = f.get("separator");
                SeparatorConfig sep = new SeparatorConfig();
                sep.setDecimal(s.getOrElse("decimal", ","));
                sep.setGroup(s.getOrElse("group", "."));
                sep.setSingle(s.getOrElse("single", " "));
                fmt.setSeparator(sep);
            }

            if (f.contains("multiples")) {
                UnmodifiableConfig m = f.get("multiples");
                MultiplesConfig mul = new MultiplesConfig();
                mul.setEnabled(m.getOrElse("enabled", true));
                mul.setStart(m.getOrElse("start", 1000));
                java.util.List<String> list = m.get("multiples");
                if (list == null) list = java.util.List.of("K","M","B","T");
                mul.setMultiples(list);
                fmt.setMultiples(mul);
            }

            c.setFormat(fmt);
        }

        // payment
        if (cfg.contains("payment")) {
            UnmodifiableConfig p = cfg.get("payment");
            PaymentConfig pay = new PaymentConfig();
            pay.setToggle(p.getOrElse("toggle", true));
            pay.setFee(p.getOrElse("fee", 0.0));
            c.setPayment(pay);
        }

        // commands
        if (cfg.contains("commands")) {
            UnmodifiableConfig cm = cfg.get("commands");
            CommandsConfig cmds = new CommandsConfig();
            cmds.setMain(cm.getOrElse("main", java.util.List.of(id)));
            cmds.setPermission(cm.getOrElse("permission", "nighteconomy."+id+".use"));

            if (cm.contains("subcommands")) {
                UnmodifiableConfig sc = cm.get("subcommands");
                SubcommandsConfig subs = new SubcommandsConfig();

                // util para ler um SubcommandConfig
                java.util.function.Function<String, SubcommandConfig> readSub = key -> {
                    if (!sc.contains(key)) return null;
                    UnmodifiableConfig sub = sc.get(key);
                    SubcommandConfig out = new SubcommandConfig();
                    out.setAliases(sub.getOrElse("aliases", java.util.List.of()));
                    out.setPermission(sub.getOrElse("permission", ""));
                    out.setPermissions(sub.getOrElse("permissions", java.util.List.of()));
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
                    tcfg.setAliases(tr.getOrElse("aliases", java.util.List.of()));
                    tcfg.setPermission(tr.getOrElse("permission", ""));
                    tcfg.setPermissions(tr.getOrElse("permissions", java.util.List.of()));

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
                        types.setExternalRemove(ty.getOrElse("externalRemove", "EXTERNAL_REMOVE"));
                        tcfg.setTypes(types);
                    }
                    subs.setTransactions(tcfg);
                }

                cmds.setSubcommands(subs);
            }

            c.setCommands(cmds);
        }

        // messages
        java.util.Map<String,String> msgs = cfg.get("messages");
        if (msgs == null) msgs = new HashMap<>();
        c.setMessages(msgs);

        return c;
    }
    public void saveToFile(Path file) {
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(file)
                .preserveInsertionOrder()
                .sync()
                .writingMode(WritingMode.REPLACE)
                .build()) {

            CommentedConfig root = cfg; // é mutável

            root.set("id", getId());
            root.set("name", getName());
            root.set("defaultValue", getDefaultValue());
            root.set("ranking", isRanking());
            root.set("update", getUpdate());
            root.set("magnata", getMagnata());

            if (getFormat() != null) {
                CommentedConfig f = CommentedConfig.inMemory();
                f.set("format", getFormat().getFormat());

                if (getFormat().getSeparator() != null) {
                    CommentedConfig s = CommentedConfig.inMemory();
                    s.set("decimal", getFormat().getSeparator().getDecimal());
                    s.set("group", getFormat().getSeparator().getGroup());
                    s.set("single", getFormat().getSeparator().getSingle());
                    f.set("separator", s);
                }

                if (getFormat().getMultiples() != null) {
                    CommentedConfig m = CommentedConfig.inMemory();
                    m.set("enabled", getFormat().getMultiples().isEnabled());
                    m.set("start", getFormat().getMultiples().getStart());
                    m.set("multiples", getFormat().getMultiples().getMultiples());
                    f.set("multiples", m);
                }

                root.set("format", f);
            }

            if (getPayment() != null) {
                CommentedConfig p = CommentedConfig.inMemory();
                p.set("toggle", getPayment().isToggle());
                p.set("fee", getPayment().getFee());
                root.set("payment", p);
            }

            if (getCommands() != null) {
                CommentedConfig cm = CommentedConfig.inMemory();
                cm.set("main", getCommands().getMain());
                cm.set("permission", getCommands().getPermission());

                if (getCommands().getSubcommands() != null) {
                    CommentedConfig sc = CommentedConfig.inMemory();

                    java.util.function.BiConsumer<String, SubcommandConfig> putSub = (key, sub) -> {
                        if (sub == null) return;
                        CommentedConfig s = CommentedConfig.inMemory();
                        if (sub.getAliases() != null) s.set("aliases", sub.getAliases());
                        if (sub.getPermission() != null) s.set("permission", sub.getPermission());
                        if (sub.getPermissions() != null) s.set("permissions", sub.getPermissions());
                        sc.set(key, s);
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
                        if (tr.getAliases() != null) tcfg.set("aliases", tr.getAliases());
                        if (tr.getPermission() != null) tcfg.set("permission", tr.getPermission());
                        if (tr.getPermissions() != null) tcfg.set("permissions", tr.getPermissions());

                        if (tr.getTypes() != null) {
                            CommentedConfig ty = CommentedConfig.inMemory();
                            ty.set("all", tr.getTypes().getAll());
                            ty.set("add", tr.getTypes().getAdd());
                            ty.set("remove", tr.getTypes().getRemove());
                            ty.set("set", tr.getTypes().getSet());
                            ty.set("reset", tr.getTypes().getReset());
                            ty.set("paySend", tr.getTypes().getPaySend());
                            ty.set("payReceive", tr.getTypes().getPayReceive());
                            ty.set("externalAdd", tr.getTypes().getExternalAdd());
                            ty.set("externalRemove", tr.getTypes().getExternalRemove());
                            tcfg.set("types", ty);
                        }

                        sc.set("transactions", tcfg);
                    }

                    cm.set("subcommands", sc);
                }

                root.set("commands", cm);
            }

            if (getMessages() != null) {
                root.set("messages", toNode(this.messages));
            }

            cfg.save();
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


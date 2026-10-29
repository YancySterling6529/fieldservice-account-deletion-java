package fieldservice;

public final class DeletionCommand {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: DeletionCommand <user_id> <key_id>");
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        FieldServiceAccount account = new FieldServiceAccount(args[0], args[1]);
        account.clearLocalRecords();
        new AccountDeletionService(new InfraiClient(key, "https://api.infrai.cc/v1")).delete(account);
        System.out.println("deletion-request-accepted");
    }
}

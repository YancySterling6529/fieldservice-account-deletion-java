package fieldservice;

public final class AccountDeletionServiceTest {
    public static void main(String[] args) throws Exception {
        FieldServiceAccount account = new FieldServiceAccount("tech-1042", "key-7788");
        account.workOrders().add(new FieldServiceAccount.WorkOrder("wo-1", java.util.List.of("photo-1"), "dispatched", "call customer"));
        boolean rejected = false;
        try { new AccountDeletionService(null).delete(account); }
        catch (IllegalStateException expected) { rejected = true; }
        if (!rejected) throw new AssertionError("live work order must block deletion");
        account.clearLocalRecords();
        if (account.hasLocalRecords()) throw new AssertionError("local records remain");
        System.out.println("account-deletion-decision-ok");
    }
}

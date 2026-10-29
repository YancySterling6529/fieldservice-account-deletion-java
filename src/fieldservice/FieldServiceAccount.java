package fieldservice;

import java.util.ArrayList;
import java.util.List;

public final class FieldServiceAccount {
    private final String userId;
    private final String keyId;
    private final List<WorkOrder> workOrders = new ArrayList<>();

    public FieldServiceAccount(String userId, String keyId) {
        this.userId = userId;
        this.keyId = keyId;
    }

    public String userId() { return userId; }
    public String keyId() { return keyId; }
    public List<WorkOrder> workOrders() { return workOrders; }
    public boolean hasLocalRecords() { return !workOrders.isEmpty(); }
    public void clearLocalRecords() { workOrders.clear(); }

    public record WorkOrder(String id, List<String> photos, String dispatchStatus, String technicianFollowUp) {}
}

package fieldservice;

import java.io.IOException;

public final class AccountDeletionService {
    private final InfraiClient infrai;

    public AccountDeletionService(InfraiClient infrai) { this.infrai = infrai; }

    public void delete(FieldServiceAccount account) throws IOException, InterruptedException, InfraiException {
        if (account.hasLocalRecords()) throw new IllegalStateException("local work-order records must be cleared first");
        for (String sessionId : infrai.listSessions(account.userId())) infrai.revokeSession(sessionId);
        infrai.revokeKey(account.keyId());
    }
}

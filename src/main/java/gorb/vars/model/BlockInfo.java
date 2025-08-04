package gorb.vars.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockInfo {
    public String blockingUser;
    public String blockedUser;
}

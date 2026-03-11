package github.liang118;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Hello implements Serializable {
    private String message;
    private String description;
}


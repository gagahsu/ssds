package com.example.ssds.infra.entity.id;

import java.io.Serializable;
import lombok.*;

/** category_audience_mix 的複合主鍵（category_id, audience_id）。 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CategoryAudienceMixId implements Serializable {

    private Long category;
    private Long audience;
}

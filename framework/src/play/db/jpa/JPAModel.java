package play.db.jpa;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Try play.db.jpa.Model
 */
@MappedSuperclass
@Deprecated
public class JPAModel extends JPASupport {

    @Id
    @GeneratedValue
    public Long id;
    
    public Long getId() {
        return id;
    }

}

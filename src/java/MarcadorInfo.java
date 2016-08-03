
import java.io.Serializable;


public class MarcadorInfo implements Serializable{

    private int point_ID =0;
    MarcadorInfo(int _pointID ) {
        this.point_ID=  _pointID;
    }
    public int getPoint_ID() {
        return point_ID;
    }

    public void setPoint_ID(int point_ID) {
        this.point_ID = point_ID;
        
    }

}

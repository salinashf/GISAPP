
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.event.map.MarkerDragEvent;
import org.primefaces.event.map.OverlaySelectEvent;
import org.primefaces.model.map.DefaultMapModel;
import org.primefaces.model.map.LatLng;
import org.primefaces.model.map.MapModel;
import org.primefaces.model.map.Marker;
import utilidades.conexion.db;

@ManagedBean(name = "ctrlMarcador")
@ViewScoped
public class beanControl implements Serializable {

    private MapModel emptyModel;
    private String title;
    private String titleNuevo;

    public String getTitleNuevo() {
        return titleNuevo;
    }

    public void setTitleNuevo(String titleNuevo) {
        this.titleNuevo = titleNuevo;
    }
    private double lat;
    private double lng;
    private Marker marcadorActual;

    public Marker getMarcadorActual() {
        return marcadorActual;
    }

    public void setMarcadorActual(Marker marcadorActual) {
        this.marcadorActual = marcadorActual;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    @PostConstruct
    public void init() {
        emptyModel = new DefaultMapModel();
        IniciarPuntos();
    }
   public void onMarkerDrag(MarkerDragEvent event) {
        Marker  mkF = buscarMarcador(event.getMarker().getId()) ; 
        cambiarDB(mkF, event.getMarker()); 
        marcadorActual =   mkF ;
        FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Punto cambiado: "+marcadorActual.getTitle(), ".. "   )
        );
   
   }
    public void IniciarPuntos() {
        leerDB();
    }

    public MapModel getEmptyModel() {
        return emptyModel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private int insertRegistros(Connection pcnx, String sqlStatement, Marker pLatLng) {
        PreparedStatement psta = null;
        ResultSet keyset = null;
        int currentInsert = -1 ; 
        try {
            psta = pcnx.prepareStatement(sqlStatement, Statement.RETURN_GENERATED_KEYS);
            psta.setString(1, pLatLng.getTitle());
            psta.setObject(2, pLatLng.getLatlng().getLng());
            psta.setObject(3, pLatLng.getLatlng().getLat());
            psta.executeUpdate();
            keyset = psta.getGeneratedKeys();
            while (keyset.next()) {
                currentInsert = keyset.getInt("puntos_id");
            }
            psta.close();
        } catch (SQLException ex) {
            GenereraError("Error", ex.getMessage());
            Logger.getLogger(beanControl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                keyset.close();
            } catch (SQLException ex) {
                Logger.getLogger(beanControl.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (psta != null) {
                try {
                    if (!psta.isClosed()) {
                        psta.close();
                    }
                } catch (SQLException ex) {
                    GenereraError("Error", ex.getMessage());
                    Logger.getLogger(beanControl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return  currentInsert;
    }

    public int  guardarDB(Marker mk) {
        int insertID   =  -1 ;
        final Connection cnx;
        try {
            cnx = db.getConectar();
            String insertPointSQL = "INSERT INTO ciudades  (nombre , geom) values( ? , (ST_SetSRID(ST_MakePoint(?,?), 4326)))";
           insertID = insertRegistros(cnx, insertPointSQL, mk);
        } finally {
            try {
                db.cerrarConexion();
            } catch (SQLException ex) {
                GenereraError("Error", ex.getMessage());
                Logger.getLogger(beanControl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return insertID;
    }

    public void leerDB() {
        final Connection cnx;
        Statement sentencia = null;
        ResultSet rs = null;
        try {
            cnx = db.getConectar();
            String selectPointSQL = "select  puntos_id ,  nombre  , ST_y(geom)  as Lat , ST_X(geom) as Lng   from   ciudades   ";
            sentencia = cnx.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = sentencia.executeQuery(selectPointSQL);

            while (rs.next()) {
                Marker puntom = new Marker(new LatLng(rs.getDouble("lat"), rs.getDouble("lng")), rs.getString("nombre")   , new MarcadorInfo(rs.getInt("puntos_id")));
                puntom.setDraggable(true);
                emptyModel.addOverlay(puntom);
            }
        } catch (SQLException ex) {
            Logger.getLogger(beanControl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (sentencia != null) {
                    sentencia.close();
                }
                db.cerrarConexion();
            } catch (SQLException ex) {
                GenereraError("Error", ex.getMessage());
                Logger.getLogger(beanControl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void GenereraError(String vmsmTitle, String vmsgContent) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, vmsmTitle, vmsgContent));
    }
    public void onMarkerSelect(OverlaySelectEvent event) {
        marcadorActual = (Marker) event.getOverlay();
        titleNuevo   =  marcadorActual.getTitle();
        Marker  mkF = buscarMarcador( ((Marker) event.getOverlay()).getId()) ; 
        mkF.setDraggable(true);
        marcadorActual =   mkF ;
    }
    
    
    
    public   void cambiarMarcador(ActionEvent actionEvent){//
      
      cambiarTituloDB(marcadorActual);
      emptyModel   =new  DefaultMapModel ();
      leerDB();
      FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "titulo Cambiado : "+marcadorActual.getTitle(), 
                        "Lat:" + marcadorActual.getLatlng().getLat() + ", Lng:" + marcadorActual.getLatlng().getLng()

                )
        );
    
    } 
    
    public   void removerMarcador(ActionEvent actionEvent){//
     elimiarDB(marcadorActual);
      
      emptyModel   =new  DefaultMapModel ();
      leerDB();
      FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Punto eliminado: "+marcadorActual.getTitle(), 
                        "Lat:" + marcadorActual.getLatlng().getLat() + ", Lng:" + marcadorActual.getLatlng().getLng()

                )
        );
    
    } 
    public Marker buscarMarcador(String  Key){
        for (Marker  x  :  emptyModel.getMarkers()){
            if (x.getId() == null ? Key == null : x.getId().equals(Key)){
            return x ;
            }  
        }
        return null ;
     } 
    
    
    public void addMarker() {
        
        MarcadorInfo mData = new MarcadorInfo(-1);
        Marker mInsert = new Marker(new LatLng(lat, lng), title, mData);
        mInsert.setDraggable(true);
        int  RowId=   guardarDB(mInsert);
        mData.setPoint_ID(RowId);
        emptyModel.addOverlay(mInsert);
        
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Punto Agregado", "Lat:" + lat + ", Lng:" + lng));
        setTitle("");
        setLat(0);
        setLng(0); 
    }

    public void cambiarDB(Marker mkNew, Marker mkOld) {
        final Connection cnx;
        PreparedStatement stm = null;
        try {
            cnx = db.getConectar();
            String deletePointSQL = "update ciudades  set  geom  =  ST_SetSRID(ST_MakePoint(?, ?), 4326)   where  puntos_id  =  ? ";
            stm = cnx.prepareStatement(deletePointSQL);
            stm.setObject(1, mkNew.getLatlng().getLng());
            stm.setObject(2, mkNew.getLatlng().getLat());
            stm.setInt(3, ((MarcadorInfo) (mkOld.getData())).getPoint_ID());
            stm.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(beanControl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stm.close();
                db.cerrarConexion();
            } catch (SQLException ex) {
                GenereraError("Error", ex.getMessage());

            }
        }
    }

    public void elimiarDB(Marker mk) {
        final Connection cnx;
        Statement stm = null;
        try {
            cnx = db.getConectar();
            String deletePointSQL = "delete from ciudades  where puntos_id  =  " + ((MarcadorInfo) (mk.getData())).getPoint_ID();
            stm = cnx.createStatement();
            stm.executeUpdate(deletePointSQL);
        } catch (SQLException ex) {
            Logger.getLogger(beanControl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stm.close();
                db.cerrarConexion();
            } catch (SQLException ex) {
                GenereraError("Error", ex.getMessage());

            }
        }
    }
    
    public void cambiarTituloDB(Marker mk) {
        final Connection cnx;
        Statement stm = null;
        try {
            cnx = db.getConectar();
            String updatePointSQL = "update ciudades  set nombre ="+ titleNuevo +" where puntos_id  =  " + ((MarcadorInfo) (mk.getData())).getPoint_ID();
            stm = cnx.createStatement();
            stm.executeUpdate(updatePointSQL);
        } catch (SQLException ex) {
            Logger.getLogger(beanControl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stm.close();
                db.cerrarConexion();
            } catch (SQLException ex) {
                GenereraError("Error", ex.getMessage());

            }
        }
    }
}

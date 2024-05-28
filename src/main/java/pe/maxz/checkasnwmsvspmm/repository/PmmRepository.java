package pe.maxz.checkasnwmsvspmm.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.log4j.Log4j2;
import pe.maxz.checkasnwmsvspmm.dto.Asn;

@Repository
@Log4j2
public class PmmRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    public List<Asn> getDiffs(List<Asn> asnsIn) throws Exception{
        List<Asn> diffs = new ArrayList<Asn>();
        String query ="";

        StringBuilder sb = new StringBuilder();
        boolean firstRow=true;
        for (var asn : asnsIn) {
            if(!firstRow) sb.append(" union all ");
            firstRow=false;
            sb.append("select '" + asn.shipment_nbr() + "' asn, '" + asn.facility_id__code() + "' facility_code from dual ");
        }
        query= "with api as (" + sb.toString()+ ") SELECT a.*, h.shipment_nbr\n" + //
                        "FROM api a, wms_rcv_asn_hdr h\n" + //
                        "WHERE a.asn=h.shipment_nbr (+)\n" + //
                        " and a.facility_code= h.facility_code (+) " 
                     //   + " and h.create_date (+) < to_date('20240523','yyyymmdd') "
                        + "and h.shipment_nbr IS null"
                        ;
        try (
            @SuppressWarnings("null")
            Connection con = jdbcTemplate.getDataSource().getConnection();
            PreparedStatement pst = con.prepareStatement(query)
        ) {
            var rs = pst.executeQuery();
            while (rs.next()) {
                var diff = new Asn(rs.getString(1), rs.getString(2));
                diffs.add(diff);
            }
        } catch (Exception e) {
            log.error(e);
            throw e;
        }
        return diffs;
    }
}

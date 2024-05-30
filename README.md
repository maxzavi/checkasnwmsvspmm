# Valida ASN WMS versus PMM

## Configuración:

```yml
spring:
  application:
    name: checkasnwmsvspmm
  datasource:
    url: jdbc:oracle:thin:@IP:port:sid
    username: ****
    password: ******    
wms:
  api: 
    uribase: https://xxxxxxxx/wms/lgfapi/v10
    pagesize: 1250
    username: *****
    password: ********
report:
  print: T
logging:
  level:
    pe: INFO
```

## Obtener datos de APIS

Usamos RestClient:

```java
          RestClient restClient = RestClient.create();
          var response = restClient.get()
            .uri(uriBase  
                + "/entity/user_facility" 
                + "?cw_user_id={0}&page_size={1}&page={2}",
                userId,
                1250,
                page)
            .header("Authorization", getAuthHeader(username, password))
            .retrieve()
            .body(Result.class);
```

Para la respuesta usamos records en lugar de clases, ya que son inmutables:

```java
        record Detail (FacilityId facility_id){};
        record Result(String next_page, List<Detail> results){};
```

Para el Auth:

```java
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode( auth.getBytes(Charset.forName("US-ASCII")));
        return "Basic " + new String( encodedAuth );
```
## Base de datos

Para obtener los datos de la base de datos:

```java
public List<Asn> getDiffs(List<Asn> asnsIn){
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
        }
        return diffs;
    }
```

# Control de errores
Controlamos el error en caso de respuesta 404 - Not Found:

```java
		dateFrom.datesUntil(dateTo.plusDays(1)).forEach(dt->{

			try {
				var asns = wmsRepository.getAsns(dt);
				fileOutput.write("dataasnfull", asns, dt);
				var diffs = pmmRepository.getDiffs(asns);
				log.info("Date: {}, Asns: {}, diffs {} {}", dt, asns.size(), diffs.size(), diffs);					
			} catch (HttpClientErrorException e) {
				if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)){
					log.info("Date: {}, Asns: {}",dt, 0);					
				}else{
					throw e;
				}
			}catch(Exception e){
				log.error(e);
			}
		});
		log.info("End");
```
## Uso de argumentos

Para usar args en la app:

```java
		int year  = Integer.parseInt(yyyymmdd.substring(0, 4));
		int month = Integer.parseInt(yyyymmdd.substring(4, 6));
		int day = Integer.parseInt(yyyymmdd.substring(6, 8));
		log.debug("year {} month {} day {}", year,month,day);		
		return LocalDate.of(year,month,day);
```


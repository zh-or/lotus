package or.lotus.core.ip2reg;

import com.maxmind.db.MaxMindDbParameter;

public class MMDBInfo {
    @MaxMindDbParameter(name = "continent.geoname_id")
    public Long continentNameId;
    @MaxMindDbParameter(name = "continent.names.zh-CN")
    public String continentName;

    @MaxMindDbParameter(name = "country.iso_code")
    public String countryIsoCode;

    @MaxMindDbParameter(name = "country.geoname_id")
    public Long countryNameId;
    @MaxMindDbParameter(name = "country.names.zh-CN")
    public String countryName;
    @MaxMindDbParameter(name = "city.geoname_id")
    public Long cityNameId;
    @MaxMindDbParameter(name = "city.names.zh-CN")
    public String cityName;

    @MaxMindDbParameter(name = "location.accuracy_radius")
    public Integer accuracyRadius;

    @MaxMindDbParameter(name = "location.metro_code")
    public Integer metroCode;
    @MaxMindDbParameter(name = "location.time_zone")
    public String timeZone;

    @MaxMindDbParameter(name = "location.latitude")
    public Double latitude;
    @MaxMindDbParameter(name = "location.longitude")
    public Double longitude;
    @MaxMindDbParameter(name = "postal.code")
    public String postal;
    @MaxMindDbParameter(name = "registered_country.iso_code")
    public String registeredCountryIsoCode;
    @MaxMindDbParameter(name = "registered_country.geoname_id")
    public Long registeredCountryNameId;
    @MaxMindDbParameter(name = "registered_country.names.zh-CN")
    public String registeredCountryName;

    @MaxMindDbParameter(name = "subdivisions.iso_code")
    public String subdivisionsIsoCode;
    @MaxMindDbParameter(name = "subdivisions.geoname_id")
    public Long subdivisionsNameId;

    @MaxMindDbParameter(name = "subdivisions.names.zh-CN")
    public String subdivisionsName;

    public MMDBInfo() {
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MMDBInfo{");
        sb.append("continentNameId=").append(continentNameId);
        sb.append(", continentName='").append(continentName).append('\'');
        sb.append(", countryIsoCode='").append(countryIsoCode).append('\'');
        sb.append(", countryNameId=").append(countryNameId);
        sb.append(", countryName='").append(countryName).append('\'');
        sb.append(", cityNameId=").append(cityNameId);
        sb.append(", cityName='").append(cityName).append('\'');
        sb.append(", accuracyRadius=").append(accuracyRadius);
        sb.append(", metroCode=").append(metroCode);
        sb.append(", timeZone='").append(timeZone).append('\'');
        sb.append(", latitude=").append(latitude);
        sb.append(", longitude=").append(longitude);
        sb.append(", postal='").append(postal).append('\'');
        sb.append(", registeredCountryIsoCode='").append(registeredCountryIsoCode).append('\'');
        sb.append(", registeredCountryNameId=").append(registeredCountryNameId);
        sb.append(", registeredCountryName='").append(registeredCountryName).append('\'');
        sb.append(", subdivisionsIsoCode='").append(subdivisionsIsoCode).append('\'');
        sb.append(", subdivisionsNameId=").append(subdivisionsNameId);
        sb.append(", subdivisionsName='").append(subdivisionsName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

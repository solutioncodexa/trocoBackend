package ma.codexa.troco.mapper;

import ma.codexa.troco.dto.CustomerDTO;
import ma.codexa.troco.dto.CustomerSummaryDTO;
import ma.codexa.troco.entity.Customer;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {

    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);

    CustomerDTO toDTO(Customer customer);

    Customer toEntity(CustomerDTO customerDTO);

    @Named("toSummary")
    @Mapping(target = "city", ignore = true)
    CustomerSummaryDTO toSummaryDTO(Customer customer);

    @Named("toSummaryWithCity")
    CustomerSummaryDTO toSummaryWithCityDTO(Customer customer);
}

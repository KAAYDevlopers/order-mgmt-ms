package com.abw12.absolutefitness.ordermgmtms.mappers;

import com.abw12.absolutefitness.ordermgmtms.dto.OrderDTO;
import com.abw12.absolutefitness.ordermgmtms.entity.OrderEntity;
import com.abw12.absolutefitness.ordermgmtms.helper.OffsetDateTimeParser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface OrderMapper  extends OffsetDateTimeParser {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    @Mapping(source = "orderCreatedAt", target = "orderCreatedAt", qualifiedByName = "stringToOffsetDateTime")
    @Mapping(source = "orderModifiedAt", target = "orderModifiedAt", qualifiedByName = "stringToOffsetDateTime")
    OrderEntity dtoToEntity(OrderDTO requestDTO);
    @Mapping(source = "orderCreatedAt", target = "orderCreatedAt", qualifiedByName = "offsetDateTimeToString")
    @Mapping(source = "orderModifiedAt", target = "orderModifiedAt", qualifiedByName = "offsetDateTimeToString")
    OrderDTO entityToDto(OrderEntity dbData);
}

package com.microservices.sender.mapper;

import com.microservices.sender.entity.UserRequest;
import com.microservices.shared.dto.UserRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserRequestMapper {

    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    UserRequestDTO toDTO(UserRequest entity);

    List<UserRequestDTO> toDTOList(List<UserRequest> entities);
}

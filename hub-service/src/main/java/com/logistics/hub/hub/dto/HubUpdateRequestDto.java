package com.logistics.hub.hub.dto;

public record HubUpdateRequestDto(
        String name,
        String address,
        Double latitude,
        Double longitude
    ){
}

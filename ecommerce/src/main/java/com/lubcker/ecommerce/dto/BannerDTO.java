package com.lubcker.ecommerce.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BannerDTO {
    private int idBanner;
    private String imagen;
    private String titulo;
    private String enlace;
    private int orden;
    private boolean activo;
}

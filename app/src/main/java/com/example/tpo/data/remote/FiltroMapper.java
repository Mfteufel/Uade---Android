package com.example.tpo.data.remote;

import androidx.annotation.Nullable;

import com.example.tpo.data.SesionUsuario;
import com.example.tpo.data.remote.dto.FiltroGuardadoDto;
import com.example.tpo.model.Categoria;
import com.example.tpo.model.Cercania;
import com.example.tpo.model.EstadoArticulo;
import com.example.tpo.model.FiltroPublicaciones;
import com.example.tpo.model.OrdenPublicaciones;
import com.example.tpo.model.Zona;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FiltroMapper {

    private FiltroMapper() {
        // Clase de utilidades: no se instancia.
    }

    @Nullable
    public static String textoDe(FiltroPublicaciones filtro) {
        return filtro.getTexto().isEmpty() ? null : filtro.getTexto();
    }

    @Nullable
    public static String categoriaDe(FiltroPublicaciones filtro) {
        return filtro.getCategoria() == null ? null : filtro.getCategoria().name();
    }

    public static List<String> estadosDe(FiltroPublicaciones filtro) {
        List<String> nombres = new ArrayList<>();
        for (EstadoArticulo estado : filtro.getEstados()) {
            nombres.add(estado.name());
        }
        return nombres;
    }

    /** Resuelve MI_ZONA/ZONAS_CERCANAS contra la zona actual del usuario logueado. */
    public static List<String> zonasDe(FiltroPublicaciones filtro) {
        Zona miZona = SesionUsuario.getInstancia().getZona();
        switch (filtro.getCercania()) {
            case MI_ZONA:
                return miZona == null
                        ? Collections.emptyList()
                        : Collections.singletonList(miZona.name());
            case ZONAS_CERCANAS:
                if (miZona == null) {
                    return Collections.emptyList();
                }
                List<String> cercanas = new ArrayList<>();
                for (Zona zona : Zona.values()) {
                    if (zona.esCercanaA(miZona)) {
                        cercanas.add(zona.name());
                    }
                }
                return cercanas;
            case TODAS:
            default:
                return Collections.emptyList();
        }
    }

    public static FiltroGuardadoDto aDto(FiltroPublicaciones filtro) {
        FiltroGuardadoDto dto = new FiltroGuardadoDto();
        dto.texto = textoDe(filtro);
        dto.categoria = categoriaDe(filtro);
        dto.estados = estadosDe(filtro);
        dto.zonas = zonasDe(filtro);
        dto.precioMinimo = filtro.getPrecioMinimo();
        dto.precioMaximo = filtro.getPrecioMaximo();
        dto.orden = filtro.getOrden().name();
        return dto;
    }

    public static FiltroPublicaciones desdeDto(FiltroGuardadoDto dto) {
        FiltroPublicaciones filtro = new FiltroPublicaciones();
        filtro.setTexto(dto.texto);
        filtro.setCategoria(categoriaValida(dto.categoria));
        filtro.setEstados(estadosValidos(dto.estados));
        filtro.setPrecioMinimo(dto.precioMinimo);
        filtro.setPrecioMaximo(dto.precioMaximo);
        filtro.setCercania(cercaniaDe(dto.zonas));
        filtro.setOrden(ordenValido(dto.orden));
        return filtro;
    }

    @Nullable
    private static Categoria categoriaValida(@Nullable String nombre) {
        if (nombre == null) {
            return null;
        }
        try {
            return Categoria.valueOf(nombre);
        } catch (IllegalArgumentException excepcion) {
            return null;
        }
    }

    private static Set<EstadoArticulo> estadosValidos(@Nullable List<String> nombres) {
        Set<EstadoArticulo> estados = EnumSet.noneOf(EstadoArticulo.class);
        if (nombres == null) {
            return estados;
        }
        for (String nombre : nombres) {
            try {
                estados.add(EstadoArticulo.valueOf(nombre));
            } catch (IllegalArgumentException ignorado) {
                // Valor desconocido: se ignora en vez de romper la búsqueda guardada entera.
            }
        }
        return estados;
    }

    private static OrdenPublicaciones ordenValido(@Nullable String nombre) {
        if (nombre != null) {
            try {
                return OrdenPublicaciones.valueOf(nombre);
            } catch (IllegalArgumentException ignorado) {
                // sigue abajo con el valor por defecto
            }
        }
        return OrdenPublicaciones.RECIENTES;
    }

    private static Cercania cercaniaDe(@Nullable List<String> zonas) {
        if (zonas == null || zonas.isEmpty()) {
            return Cercania.TODAS;
        }
        Zona miZona = SesionUsuario.getInstancia().getZona();
        if (miZona == null) {
            return Cercania.TODAS;
        }
        Set<String> guardadas = new HashSet<>(zonas);
        if (guardadas.size() == 1 && guardadas.contains(miZona.name())) {
            return Cercania.MI_ZONA;
        }
        Set<String> cercanas = new HashSet<>();
        for (Zona zona : Zona.values()) {
            if (zona.esCercanaA(miZona)) {
                cercanas.add(zona.name());
            }
        }
        return guardadas.equals(cercanas) ? Cercania.ZONAS_CERCANAS : Cercania.TODAS;
    }
}

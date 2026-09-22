package com.example.tpo.di;

import com.example.tpo.data.MisPublicacionesRepository;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Puerta de entrada a Hilt para {@code PublicarArticuloViewModel}.
 * <p>
 * Ese ViewModel es un {@code AndroidViewModel} común (lo scopea manualmente el
 * nav graph del wizard), no un {@code @HiltViewModel}, así que no puede recibir
 * dependencias por {@code @Inject} directo. Este {@code @EntryPoint} es la forma
 * estándar de Hilt de exponer un binding del grafo a una clase que no es
 * Hilt-managed, sin tener que armar el repositorio a mano ni duplicar la
 * configuración de Retrofit.
 */
@EntryPoint
@InstallIn(SingletonComponent.class)
public interface PublicarEntryPoint {

    MisPublicacionesRepository misPublicacionesRepository();
}

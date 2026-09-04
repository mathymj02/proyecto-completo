// src/app/services/catalogo.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/** Forma de un producto, tal como lo devuelve catalogo-api (ver ProductoController.java del backend). */
export interface Producto {
  id?: number;
  nombre: string;
  descripcion?: string;
  precio: number;
  stock: number;
  categoria?: string;
  imagenUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private baseUrl = `${environment.apiUrls.catalogo}/productos`;

  constructor(private http: HttpClient) {}

  listar(categoria?: string): Observable<Producto[]> {
    const url = categoria ? `${this.baseUrl}?categoria=${categoria}` : this.baseUrl;
    return this.http.get<Producto[]>(url);
  }

  obtener(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.baseUrl}/${id}`);
  }

  crear(producto: Producto): Observable<Producto> {
    return this.http.post<Producto>(this.baseUrl, producto);
  }

  actualizar(id: number, producto: Producto): Observable<Producto> {
    return this.http.put<Producto>(`${this.baseUrl}/${id}`, producto);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

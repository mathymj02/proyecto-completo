// src/app/services/carrito.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ItemCarritoRequest {
  productoId: number;
  nombreProducto: string;
  precioUnitario: number;
  cantidad: number;
}

@Injectable({ providedIn: 'root' })
export class CarritoService {
  private baseUrl = environment.apiUrls.carrito;

  constructor(private http: HttpClient) {}

  obtenerMiCarrito(): Observable<any> {
    return this.http.get(`${this.baseUrl}/mios`);
  }

  agregarItem(item: ItemCarritoRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/items`, item);
  }

  actualizarCantidad(itemId: number, cantidad: number): Observable<any> {
    return this.http.put(`${this.baseUrl}/items/${itemId}`, { cantidad });
  }

  eliminarItem(itemId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/items/${itemId}`);
  }

  vaciarCarrito(): Observable<any> {
    return this.http.delete(`${this.baseUrl}/mios`);
  }
}

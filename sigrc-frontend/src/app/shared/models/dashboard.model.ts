export interface Dashboard {
  ticketsAbiertos: number;
  ticketsCerrados: number;
  ticketsVencidos: number;
  ticketsSinAsignar: number;
  tiempoPromedioAtencionHoras: number;
  cumplimientoSLA: number;
  ticketsPorEstado: ChartItem[];
  ticketsPorPrioridad: ChartItem[];
  ticketsPorArea: ChartItem[];
  ticketsPorSistema: ChartItem[];
  ticketsPorTipo: ChartItem[];
  tendenciasMensuales: ChartItem[];
}

export interface ChartItem {
  [key: string]: string | number;
}

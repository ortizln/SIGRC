import { Component, HostListener } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from './sidebar.component';
import { HeaderComponent } from './header.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, HeaderComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css'
})
export class MainLayoutComponent {
  sidebarCollapsed = false;
  sidebarMobileOpen = false;

  constructor() {
    this.checkScreenSize();
  }

  @HostListener('window:resize')
  checkScreenSize() {
    const width = window.innerWidth;
    if (width >= 1024) {
      this.sidebarCollapsed = false;
      this.sidebarMobileOpen = false;
    } else if (width >= 768) {
      this.sidebarCollapsed = true;
      this.sidebarMobileOpen = false;
    } else {
      this.sidebarCollapsed = false;
      this.sidebarMobileOpen = false;
    }
  }

  toggleSidebar() {
    const width = window.innerWidth;
    if (width >= 768 && width < 1024) {
      this.sidebarCollapsed = !this.sidebarCollapsed;
    } else {
      this.sidebarMobileOpen = !this.sidebarMobileOpen;
    }
  }

  onSidebarNavClick() {
    if (window.innerWidth < 1024) {
      this.sidebarMobileOpen = false;
    }
  }
}

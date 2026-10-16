import React, { useState, useEffect } from 'react';
import { soundEngine } from '../utils/audio';
import { Download, Smartphone, Monitor, ShieldCheck, X, CheckCircle, ExternalLink, Sparkles } from 'lucide-react';

interface ApkModalProps {
  onClose: () => void;
}

export const ApkModal: React.FC<ApkModalProps> = ({ onClose }) => {
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [installed, setInstalled] = useState(false);
  const [activeTab, setActiveTab] = useState<'apk' | 'pwa' | 'pc'>('apk');

  useEffect(() => {
    const handler = (e: any) => {
      e.preventDefault();
      setDeferredPrompt(e);
    };
    window.addEventListener('beforeinstallprompt', handler);
    return () => window.removeEventListener('beforeinstallprompt', handler);
  }, []);

  const handleInstallPWA = async () => {
    soundEngine.playKeyType();
    if (deferredPrompt) {
      deferredPrompt.prompt();
      const choiceResult = await deferredPrompt.userChoice;
      if (choiceResult.outcome === 'accepted') {
        setInstalled(true);
      }
      setDeferredPrompt(null);
    } else {
      alert('Para instalar en Android / PC: Presiona el menú del navegador (⋮) y selecciona "Instalar aplicación" o "Agregar a pantalla principal".');
    }
  };

  const handleToggleFullscreen = () => {
    soundEngine.playKeyType();
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
    } else {
      document.exitFullscreen().catch(() => {});
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-4 animate-in fade-in">
      <div className="w-full max-w-xl bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl relative max-h-[90vh] overflow-y-auto">
        {/* Close Button */}
        <button
          onClick={() => {
            soundEngine.playKeyType();
            onClose();
          }}
          className="absolute top-5 right-5 p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition-colors"
          aria-label="Cerrar modal"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header */}
        <div className="flex items-center gap-3.5 mb-6">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-cyan-400 via-blue-600 via-purple-600 to-violet-500 p-[2.5px] shadow-[0_0_20px_rgba(6,182,212,0.4)] flex items-center justify-center">
            <div className="w-full h-full bg-slate-950 rounded-[14px] flex items-center justify-center">
              <Smartphone className="w-6 h-6 text-cyan-400" />
            </div>
          </div>
          <div>
            <h2 className="font-display font-extrabold text-xl text-white flex items-center gap-2">
              <span>Descargar APK & Jugar en PC</span>
              <span className="text-[10px] px-2 py-0.5 rounded-full font-mono bg-cyan-950 border border-cyan-400/50 text-cyan-300">
                v1.0.0
              </span>
            </h2>
            <p className="text-xs text-slate-400">
              Instalador nativo para Android y experiencia optimizada en computadoras
            </p>
          </div>
        </div>

        {/* Tab Selector */}
        <div className="grid grid-cols-3 gap-2 p-1 bg-slate-950 rounded-2xl border border-slate-800 mb-6">
          <button
            onClick={() => {
              soundEngine.playKeyType();
              setActiveTab('apk');
            }}
            className={`flex items-center justify-center gap-1.5 py-2 rounded-xl text-xs font-bold transition-all ${
              activeTab === 'apk'
                ? 'bg-rose-600 text-white shadow-lg'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Download className="w-3.5 h-3.5" />
            <span>Descargar APK</span>
          </button>

          <button
            onClick={() => {
              soundEngine.playKeyType();
              setActiveTab('pwa');
            }}
            className={`flex items-center justify-center gap-1.5 py-2 rounded-xl text-xs font-bold transition-all ${
              activeTab === 'pwa'
                ? 'bg-cyan-600 text-white shadow-lg'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span>Instalar WebAPK</span>
          </button>

          <button
            onClick={() => {
              soundEngine.playKeyType();
              setActiveTab('pc');
            }}
            className={`flex items-center justify-center gap-1.5 py-2 rounded-xl text-xs font-bold transition-all ${
              activeTab === 'pc'
                ? 'bg-purple-600 text-white shadow-lg'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Monitor className="w-3.5 h-3.5" />
            <span>Jugar en PC</span>
          </button>
        </div>

        {/* TAB 1: APK Download */}
        {activeTab === 'apk' && (
          <div className="space-y-4">
            <div className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm font-bold text-white">STOP_Multiplayer_v1.0.apk</div>
                  <div className="text-xs text-slate-400 font-mono">Paquete: com.compuapps.stopgame · Android 5.0+</div>
                </div>
                <span className="text-[11px] px-2 py-0.5 rounded bg-emerald-950 border border-emerald-500/40 text-emerald-400 font-semibold flex items-center gap-1">
                  <ShieldCheck className="w-3.5 h-3.5" /> Verificado
                </span>
              </div>

              {/* Direct Download Button */}
              <a
                href="/api/download/stop-game.apk"
                download="STOP_Multiplayer_v1.0.apk"
                onClick={() => soundEngine.playLetterReveal()}
                className="w-full flex items-center justify-center gap-2 py-3.5 px-4 rounded-2xl bg-gradient-to-r from-rose-500 via-rose-600 to-amber-600 hover:from-rose-400 hover:to-amber-500 text-white font-display font-extrabold text-sm shadow-[0_0_25px_rgba(244,63,94,0.4)] transition-all hover:scale-[1.02] active:scale-95"
              >
                <Download className="w-4 h-4" />
                <span>Descargar APK Directo para Android</span>
              </a>
            </div>

            {/* Steps Guide */}
            <div className="p-4 rounded-2xl bg-slate-950/40 border border-slate-800 text-xs text-slate-300 space-y-2.5">
              <div className="font-bold text-slate-200 uppercase tracking-wider text-[11px] flex items-center gap-1.5 text-cyan-400">
                <Sparkles className="w-3.5 h-3.5" /> Pasos para instalar en Android:
              </div>
              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-slate-800 text-rose-400 font-bold flex items-center justify-center shrink-0">1</span>
                <span>Presiona el botón de descarga y abre el archivo <code className="text-rose-300">STOP_Multiplayer_v1.0.apk</code> en tus descargas.</span>
              </div>
              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-slate-800 text-rose-400 font-bold flex items-center justify-center shrink-0">2</span>
                <span>Si Android muestra un aviso de seguridad, toca <strong>Configuración</strong> y activa <strong>Permitir desde esta fuente</strong>.</span>
              </div>
              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-slate-800 text-rose-400 font-bold flex items-center justify-center shrink-0">3</span>
                <span>Presiona <strong>Instalar</strong> y disfruta del juego multijugador con tus amigos.</span>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: PWA WebAPK */}
        {activeTab === 'pwa' && (
          <div className="space-y-4">
            <div className="p-4 rounded-2xl bg-slate-950/70 border border-cyan-500/30 space-y-3">
              <div className="text-sm font-bold text-white flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-cyan-400" />
                <span>Instalación Automática Oficial (WebAPK)</span>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                Google Chrome y Android admiten instalación instantánea sin descargar archivos desconocidos. Se agrega a tu cajón de aplicaciones como una app nativa con icono propio, pantalla completa y soporte sin conexión.
              </p>

              <button
                onClick={handleInstallPWA}
                className="w-full flex items-center justify-center gap-2 py-3.5 px-4 rounded-2xl bg-gradient-to-r from-cyan-500 via-blue-600 to-purple-600 hover:from-cyan-400 hover:to-purple-500 text-white font-display font-extrabold text-sm shadow-[0_0_25px_rgba(6,182,212,0.4)] transition-all hover:scale-[1.02] active:scale-95"
              >
                <Smartphone className="w-4 h-4" />
                <span>{installed ? '¡Aplicación Instalada!' : 'Instalar en Pantalla Principal / App'}</span>
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950/40 border border-slate-800 text-xs text-slate-400">
              💡 <strong>En iPhone / Safari:</strong> Toca el botón <strong>Compartir</strong> (icono de cuadrado con flecha arriba) y selecciona <strong>"Agregar a Inicio"</strong>.
            </div>
          </div>
        )}

        {/* TAB 3: PC Web Experience */}
        {activeTab === 'pc' && (
          <div className="space-y-4">
            <div className="p-4 rounded-2xl bg-slate-950/70 border border-purple-500/30 space-y-3">
              <div className="text-sm font-bold text-white flex items-center gap-1.5">
                <Monitor className="w-4 h-4 text-purple-400" />
                <span>100% Funcional en PC desde la Web</span>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                Puedes jugar en cualquier PC o laptop (Windows, Mac, Linux) abriendo esta página web. Disfruta de la mejor experiencia con teclado físico, atajos rápidos y pantalla completa.
              </p>

              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                  <div className="font-bold text-slate-200">⌨️ Teclado Rápido</div>
                  <div className="text-[11px] text-slate-400 mt-1">Usa <kbd className="px-1 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">Enter</kbd> para avanzar al siguiente campo al instante.</div>
                </div>
                <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                  <div className="font-bold text-slate-200">⚡ Atajos de Juego</div>
                  <div className="text-[11px] text-slate-400 mt-1"><kbd className="px-1 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">Esc</kbd> para pausar, <kbd className="px-1 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">M</kbd> para silenciar sonido.</div>
                </div>
              </div>

              <button
                onClick={handleToggleFullscreen}
                className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs border border-slate-700 transition-colors"
              >
                <Monitor className="w-3.5 h-3.5" />
                <span>Activar / Salir de Pantalla Completa</span>
              </button>
            </div>
          </div>
        )}

        {/* Footer Credit */}
        <div className="mt-6 pt-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-500 font-mono">
          <span>createby CompuApps SecureDev</span>
          <span>STOP! Multiplayer v1.0.0</span>
        </div>
      </div>
    </div>
  );
};

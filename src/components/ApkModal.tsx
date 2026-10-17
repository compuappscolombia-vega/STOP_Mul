import React, { useState, useEffect } from 'react';
import { soundEngine } from '../utils/audio';
import { Download, Smartphone, Monitor, ShieldCheck, X, CheckCircle, Sparkles, AlertCircle, Terminal, HelpCircle } from 'lucide-react';

interface ApkModalProps {
  onClose: () => void;
}

export const ApkModal: React.FC<ApkModalProps> = ({ onClose }) => {
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [installed, setInstalled] = useState(false);
  const [activeTab, setActiveTab] = useState<'pwa' | 'apk' | 'pc'>('pwa');
  const [showManualGuide, setShowManualGuide] = useState(false);

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
      setShowManualGuide(true);
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
              <span>Instalar STOP! en Android & PC</span>
              <span className="text-[10px] px-2 py-0.5 rounded-full font-mono bg-cyan-950 border border-cyan-400/50 text-cyan-300">
                v1.0.0
              </span>
            </h2>
            <p className="text-xs text-slate-400">
              Instalador WebAPK oficial para Android y experiencia de escritorio para PC
            </p>
          </div>
        </div>

        {/* Tab Selector */}
        <div className="grid grid-cols-3 gap-2 p-1 bg-slate-950 rounded-2xl border border-slate-800 mb-6">
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
            <span>App Android</span>
          </button>

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
            <span>Compilar APK</span>
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

        {/* TAB 1: PWA WebAPK (Primary / Recommended) */}
        {activeTab === 'pwa' && (
          <div className="space-y-4">
            <div className="p-4 rounded-2xl bg-slate-950/70 border border-cyan-500/30 space-y-3">
              <div className="flex items-center justify-between">
                <div className="text-sm font-bold text-white flex items-center gap-1.5">
                  <span className="w-2.5 h-2.5 rounded-full bg-cyan-400 animate-pulse" />
                  <span>Instalación Nativa en Android (WebAPK)</span>
                </div>
                <span className="text-[11px] px-2 py-0.5 rounded bg-emerald-950 border border-emerald-500/40 text-emerald-400 font-semibold flex items-center gap-1">
                  <ShieldCheck className="w-3.5 h-3.5" /> 100% Funcional
                </span>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                Google Chrome y Android compilan e instalan directamente un paquete <strong>WebAPK nativo</strong> en tu teléfono. Se añade a la pantalla de inicio y al cajón de aplicaciones con icono propio, sin barra de direcciones y con soporte sin conexión.
              </p>

              <button
                onClick={handleInstallPWA}
                className="w-full flex items-center justify-center gap-2 py-3.5 px-4 rounded-2xl bg-gradient-to-r from-cyan-500 via-blue-600 to-purple-600 hover:from-cyan-400 hover:to-purple-500 text-white font-display font-extrabold text-sm shadow-[0_0_25px_rgba(6,182,212,0.4)] transition-all hover:scale-[1.02] active:scale-95 cursor-pointer"
              >
                <Smartphone className="w-4 h-4" />
                <span>{installed ? '¡Aplicación Instalada!' : 'Instalar App en Android / PC'}</span>
              </button>
            </div>

            {/* Manual step guidance if deferred prompt is not immediately ready */}
            {showManualGuide && (
              <div className="p-4 rounded-2xl bg-cyan-950/30 border border-cyan-500/40 text-xs text-slate-200 space-y-2 animate-in fade-in">
                <div className="font-bold text-cyan-300 flex items-center gap-1.5 text-[11px] uppercase tracking-wider">
                  <HelpCircle className="w-4 h-4" /> Cómo instalar paso a paso:
                </div>
                <div className="space-y-1.5 text-xs text-slate-300">
                  <p>• <strong>En Chrome o Edge (Android):</strong> Toca los tres puntos <code className="text-cyan-300 bg-slate-900 px-1 py-0.5 rounded font-mono">⋮</code> en la esquina superior derecha y selecciona <strong>"Instalar aplicación"</strong> o <strong>"Agregar a la pantalla principal"</strong>.</p>
                  <p>• <strong>En Samsung Internet:</strong> Toca el menú <code className="text-cyan-300 bg-slate-900 px-1 py-0.5 rounded font-mono">☰</code> y presiona <strong>"Agregar página a" → "Pantalla de inicio"</strong>.</p>
                  <p>• <strong>En iPhone / Safari:</strong> Toca el botón <strong>Compartir</strong> (icono de cuadrado con flecha) y elige <strong>"Agregar a Inicio"</strong>.</p>
                </div>
              </div>
            )}

            <div className="p-3.5 rounded-xl bg-slate-950/40 border border-slate-800 text-xs text-slate-400">
              💡 <strong>Ventaja sobre un archivo APK descargado:</strong> No requiere activar "orígenes desconocidos", no da error de análisis de paquete y se actualiza automáticamente con nuevas funciones del juego.
            </div>
          </div>
        )}

        {/* TAB 2: APK Standalone Information */}
        {activeTab === 'apk' && (
          <div className="space-y-4">
            <div className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm font-bold text-white">¿Por qué un archivo .apk manual da error?</div>
                  <div className="text-xs text-slate-400">Explicación técnica de compatibilidad de Android</div>
                </div>
                <span className="text-[11px] px-2 py-0.5 rounded bg-amber-950 border border-amber-500/40 text-amber-300 font-semibold flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" /> Información
                </span>
              </div>

              <p className="text-xs text-slate-300 leading-relaxed">
                Android moderno (Android 8 al 15) exige que cualquier archivo <code className="text-rose-300 font-mono">.apk</code> descargado esté compilado binariamente con el <strong>Android SDK de Google</strong> (AAPT2 para el AndroidManifest.xml y D8 para el DEX de bytecode) y firmado criptográficamente con una clave oficial de desarrollador (<code className="text-rose-300 font-mono">apksigner</code>).
              </p>
              <p className="text-xs text-slate-300 leading-relaxed">
                Un archivo empaquetado solo como zip produce el mensaje <em>"Hubo un problema al analizar el paquete"</em>.
              </p>
            </div>

            {/* Recommended solution */}
            <div className="p-4 rounded-2xl bg-slate-950/40 border border-slate-800 text-xs text-slate-300 space-y-2.5">
              <div className="font-bold text-cyan-400 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5" /> La solución correcta para jugar:
              </div>
              <p className="text-xs text-slate-300">
                Usa la pestaña <strong>"App Android"</strong>. Al presionar <strong>"Instalar App"</strong>, Android usa su motor nativo WebAPK para crear e instalar la aplicación verificada directamente en tu teléfono sin errores.
              </p>
            </div>

            {/* Developer CLI Guide */}
            <div className="p-4 rounded-2xl bg-slate-950/60 border border-slate-800 text-xs text-slate-300 space-y-2">
              <div className="font-bold text-amber-400 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                <Terminal className="w-3.5 h-3.5" /> Para desarrolladores (Compilar APK nativo firmado para Play Store):
              </div>
              <p className="text-xs text-slate-400">
                Puedes compilar un APK/AAB firmado oficial usando la herramienta CLI de Google <strong>Bubblewrap</strong>:
              </p>
              <div className="bg-slate-950 p-2.5 rounded-xl font-mono text-[11px] text-cyan-300 overflow-x-auto border border-slate-800">
                npx @bubblewrap/cli init --manifest=https://ais-dev-5sgodu42ehmizkp5hx42sm-466582694499.us-west2.run.app/manifest.json<br/>
                npx @bubblewrap/cli build
              </div>
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
                className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs border border-slate-700 transition-colors cursor-pointer"
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

import React, { useState, useEffect } from 'react';

/**
 * MinimalDigitalClock Component
 * 
 * A large, clean, minimalist digital clock designed with a modern feature-phone aesthetic
 * (inspired by Nothing OS, Braun minimalist grid layout, and classic e-ink devices).
 * 
 * Features:
 * - Ultra-crisp monospace font pairings
 * - Live ticking seconds & blinking state colon animation
 * - Feature-phone status bar simulation (Signal, Cellular operator, Battery widget)
 * - Retro-style circular dot matrix visual representation of current minute progress (60 dot nodes)
 * - 12-hour or 24-hour format dynamic switching
 */

export interface ClockProps {
  initialFormat12h?: boolean;
}

export const MinimalDigitalClock: React.FC<ClockProps> = ({ initialFormat12h = false }) => {
  const [time, setTime] = useState(new Date());
  const [is12h, setIs12h] = useState(initialFormat12h);
  const [isColonVisible, setIsColonVisible] = useState(true);

  useEffect(() => {
    // 1-second accurate timer logic
    const clockInterval = setInterval(() => {
      setTime(new Date());
    }, 1000);

    // Precise 500ms ticker for blinking hardware colon state
    const colonInterval = setInterval(() => {
      setIsColonVisible((prev) => !prev);
    }, 500);

    return () => {
      clearInterval(clockInterval);
      clearInterval(colonInterval);
    };
  }, []);

  const formatHours = (hours: number): string => {
    if (is12h) {
      const h = hours % 12;
      return (h === 0 ? 12 : h).toString().padStart(2, '0');
    }
    return hours.toString().padStart(2, '0');
  };

  const hoursStr = formatHours(time.getHours());
  const minutesStr = time.getMinutes().toString().padStart(2, '0');
  const secondsStr = time.getSeconds().toString().padStart(2, '0');
  const ampmStr = time.getHours() >= 12 ? 'PM' : 'AM';

  // Format Date (e.g. "SUN • JUN 14")
  const days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
  const months = ['JUN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
  const dayName = days[time.getDay()];
  const monthName = months[time.getMonth()];
  const dayNum = time.getDate().toString().padStart(2, '0');

  // Calculates percentage fraction of the current minute (0.0 to 1.0)
  const secondsProgress = time.getSeconds() / 60;

  return (
    <div className="flex flex-col items-center justify-center min-h-[480px] w-full max-w-sm mx-auto bg-[#0A0A0B] text-white p-6 font-mono border-4 border-[#1C1C1E] rounded-[42px] shadow-2xl relative overflow-hidden select-none">
      
      {/* 1. Feature Phone Status Line (Retro Status Bar) */}
      <div className="w-full flex items-center justify-between text-[10px] text-zinc-500 font-bold tracking-wider border-b border-zinc-900 pb-3 mb-6">
        {/* Connection status dots */}
        <div className="flex items-center gap-1.5">
          <div className="flex gap-0.5 items-end h-[8px]">
            <div className="w-[2px] h-[3px] bg-emerald-500 rounded-sm"></div>
            <div className="w-[2px] h-[5px] bg-emerald-500 rounded-sm"></div>
            <div className="w-[2px] h-[7px] bg-emerald-500 rounded-sm"></div>
            <div className="w-[2px] h-[9px] bg-emerald-500 rounded-sm"></div>
          </div>
          <span>ZEN LOCK_</span>
        </div>

        {/* Battery widget */}
        <div className="flex items-center gap-2">
          <span>87%</span>
          <div className="w-[18px] h-[9px] border border-zinc-500 rounded-[2px] p-[1px] flex items-center relative">
            <div className="h-full bg-zinc-300 rounded-[1px]" style={{ width: '87%' }}></div>
            <div className="w-[1px] h-[3px] bg-zinc-500 absolute -right-[2px] rounded-r-xs"></div>
          </div>
        </div>
      </div>

      {/* 2. Clock Mode Switch (12H / 24H) */}
      <button 
        onClick={() => setIs12h(!is12h)}
        className="px-3 py-1 text-[9px] font-bold text-zinc-400 bg-zinc-900 rounded-full border border-zinc-800 hover:bg-zinc-800 transition duration-150 tracking-widest active:scale-95 mb-4"
      >
        {is12h ? 'FORMAT 24H' : 'FORMAT 12H'}
      </button>

      {/* 3. Main Digital Time Screen */}
      <div className="flex flex-col items-center justify-center my-4 py-3 w-full bg-[#111112] border border-zinc-800/60 rounded-2xl shadow-inner relative px-4">
        
        {/* Glow backlight shadow */}
        <div className="absolute inset-0 bg-zinc-500/2 opacity-[0.01] pointer-events-none rounded-2xlBlur"></div>
        
        <div className="flex items-baseline justify-center select-none font-black text-white">
          {/* Hour display */}
          <span className="text-6xl tracking-tight leading-none tabular-nums select-all">
            {hoursStr}
          </span>
          
          {/* Blinking physical colon */}
          <span 
            className="text-5xl px-1.5 font-bold text-[#FF3B30] inline-block leading-none select-none transition-opacity duration-150 align-top"
            style={{ opacity: isColonVisible ? 1 : 0.25 }}
          >
            :
          </span>
          
          {/* Minutes display */}
          <span className="text-6xl tracking-tight leading-none tabular-nums select-all">
            {minutesStr}
          </span>

          {/* AM / PM notation marker */}
          {is12h && (
            <span className="text-xs font-black text-[#FF3B30] tracking-widest ml-1 self-start pt-1.5">
              {ampmStr}
            </span>
          )}
        </div>

        {/* Sub-Seconds and connection status readout label */}
        <div className="mt-2 flex items-center justify-center gap-1.5 text-[8px] font-black text-zinc-400 tracking-[1.5px]">
          <div className="w-1.5 h-1.5 rounded-full bg-[#FF3B30]"></div>
          <span>SEC_{secondsStr}</span>
        </div>
      </div>

      {/* 4. Date & Day Segment Display */}
      <div className="flex items-center justify-center gap-2 py-1 px-4 my-2 text-zinc-400 font-bold text-[11px] tracking-[2px] bg-[#111112] border border-zinc-900 rounded-full">
        <span>{dayName}</span>
        <span className="text-zinc-600 text-[8px]">•</span>
        <span>{monthName} {dayNum}</span>
      </div>

      {/* 5. Minimalist Dot Matrix Interactive Progression Loop */}
      <div className="relative w-40 h-40 my-6 flex items-center justify-center group">
        
        {/* Vector Hardware segments dot track (Circular 24 dot display matching the Focus Countdown system) */}
        <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
          <circle 
            className="text-zinc-900" 
            strokeWidth="2" 
            stroke="currentColor" 
            fill="transparent" 
            r="44" 
            cx="50" 
            cy="50" 
          />
          <circle 
            className="text-[#FF3B30] transition-all duration-300" 
            strokeWidth="2.5" 
            strokeDasharray={276}
            strokeDashoffset={276 - (276 * secondsProgress)}
            strokeLinecap="round" 
            stroke="currentColor" 
            fill="transparent" 
            r="44" 
            cx="50" 
            cy="50" 
          />
        </svg>

        {/* Inside the Loop Display Container */}
        <div className="absolute flex flex-col items-center justify-center">
          <span className="text-[10px] text-zinc-500 font-bold tracking-widest">MIN_LAP</span>
          <span className="text-[13px] font-bold text-white tracking-widest tabular-nums mt-0.5">
            {Math.round(secondsProgress * 100)}%
          </span>
        </div>
      </div>

      {/* 6. Secure Offline Protocol footer labels */}
      <div className="w-full text-center mt-3 text-[9px] font-black tracking-widest text-[#FF3B30]/80">
        • COGNITIVE LOCK_PROTOCOL ACTIVE •
      </div>

      {/* Hardware Speaker slot details decorative element */}
      <div className="w-20 h-[5px] bg-[#1C1C1E] rounded-full mt-6 mb-2 border border-black/50"></div>
    </div>
  );
};

export default MinimalDigitalClock;

"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

const STORAGE_KEY = "loto_master";

/** Build the 9x10 board: columns 0-8 map to number ranges 1-9, 10-19, ..., 80-90 */
function buildBoard(): number[][] {
  const board: number[][] = [];
  for (let row = 0; row < 10; row++) {
    const cells: number[] = [];
    for (let col = 0; col < 9; col++) {
      const num = col === 0 ? row + 1 : col * 10 + row;
      // Column 0: 1-9 (row 9 is empty), Columns 1-8: 10-19, ..., 80-89 (row 9 has 90 for col 8)
      if (col === 0 && row === 9) {
        cells.push(0); // no number
      } else if (col === 8 && row === 9) {
        cells.push(90);
      } else if (col > 0 && row === 9) {
        cells.push(0); // no number
      } else {
        cells.push(num);
      }
    }
    board.push(cells);
  }
  return board;
}

interface MasterState {
  called: number[];
  remaining: number[];
}

function createFreshState(): MasterState {
  const all = Array.from({ length: 90 }, (_, i) => i + 1);
  // Shuffle
  for (let i = all.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [all[i], all[j]] = [all[j], all[i]];
  }
  return { called: [], remaining: all };
}

function saveState(state: MasterState): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function loadState(): MasterState | null {
  const data = localStorage.getItem(STORAGE_KEY);
  if (!data) return null;
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
}

const BOARD = buildBoard();

export default function MasterPage() {
  const [state, setState] = useState<MasterState | null>(null);
  const [lastCalled, setLastCalled] = useState<number | null>(null);

  useEffect(() => {
    const saved = loadState();
    if (saved && saved.called.length > 0) {
      setState(saved);
      setLastCalled(saved.called[saved.called.length - 1]);
    }
  }, []);

  useEffect(() => {
    if (state) saveState(state);
  }, [state]);

  const calledSet = new Set(state?.called ?? []);

  const handleNewGame = useCallback(() => {
    if (state && !confirm("Bạn có muốn tạo ván mới không?")) return;
    const fresh = createFreshState();
    setState(fresh);
    setLastCalled(null);
  }, [state]);

  const handleDrawNext = useCallback(() => {
    if (!state || state.remaining.length === 0) return;
    const next = state.remaining[0];
    setState({
      called: [...state.called, next],
      remaining: state.remaining.slice(1),
    });
    setLastCalled(next);
  }, [state]);

  const handleToggleNumber = useCallback(
    (num: number) => {
      if (!state) return;
      if (calledSet.has(num)) {
        // Uncall: remove from called, add back to remaining
        setState({
          called: state.called.filter((n) => n !== num),
          remaining: [...state.remaining, num],
        });
        if (lastCalled === num) {
          const newCalled = state.called.filter((n) => n !== num);
          setLastCalled(newCalled.length > 0 ? newCalled[newCalled.length - 1] : null);
        }
      } else {
        // Manual call: add to called, remove from remaining
        setState({
          called: [...state.called, num],
          remaining: state.remaining.filter((n) => n !== num),
        });
        setLastCalled(num);
      }
    },
    [state, calledSet, lastCalled]
  );

  return (
    <div className="flex flex-col flex-1 items-center px-3 py-8 sm:py-12">
      <div className="w-full max-w-2xl">
        {/* Header */}
        <header className="text-center mb-6">
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight bg-gradient-to-r from-orange-500 to-red-500 bg-clip-text text-transparent">
            Quản trò
          </h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Xổ số và theo dõi bảng lô tô
          </p>
          <Link
            href="/"
            className="mt-2 inline-block text-xs text-indigo-500 dark:text-indigo-400 hover:underline"
          >
            ← Về trang người chơi
          </Link>
        </header>

        {/* Controls */}
        <div className="flex justify-center gap-3 mb-6">
          <button
            onClick={handleNewGame}
            className="px-5 py-2.5 rounded-full font-semibold text-white text-sm
                       bg-gradient-to-r from-orange-500 to-red-500
                       hover:from-orange-600 hover:to-red-600
                       active:scale-95 transition-all shadow-lg shadow-orange-500/25"
          >
            Ván mới
          </button>
          {state && state.remaining.length > 0 && (
            <button
              onClick={handleDrawNext}
              className="px-6 py-2.5 rounded-full font-semibold text-white text-sm
                         bg-gradient-to-r from-emerald-500 to-teal-500
                         hover:from-emerald-600 hover:to-teal-600
                         active:scale-95 transition-all shadow-lg shadow-emerald-500/25"
            >
              Xổ số
            </button>
          )}
        </div>

        {/* Current number display */}
        {lastCalled && (
          <div className="flex flex-col items-center mb-6">
            <div className="text-xs uppercase tracking-widest text-slate-400 dark:text-slate-500 mb-1">
              Số vừa xổ
            </div>
            <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl bg-gradient-to-br from-orange-500 to-red-500 flex items-center justify-center shadow-xl shadow-orange-500/30">
              <span className="text-4xl sm:text-5xl font-black text-white">
                {lastCalled}
              </span>
            </div>
            {state && (
              <div className="mt-2 text-xs text-slate-400 dark:text-slate-500">
                Đã xổ: {state.called.length}/90 &middot; Còn lại: {state.remaining.length}
              </div>
            )}
          </div>
        )}

        {/* Called numbers history */}
        {state && state.called.length > 0 && (
          <div className="mb-6 px-1">
            <div className="text-xs text-slate-400 dark:text-slate-500 mb-1">
              Thứ tự đã xổ:
            </div>
            <div className="flex flex-wrap gap-1.5">
              {state.called.map((num, i) => (
                <span
                  key={i}
                  className="inline-flex items-center justify-center w-7 h-7 text-xs font-bold rounded-md bg-orange-100 dark:bg-orange-950/40 text-orange-700 dark:text-orange-300"
                >
                  {num}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Master board 9x10 */}
        {state ? (
          <div className="rounded-2xl overflow-hidden shadow-xl shadow-slate-200/50 dark:shadow-black/30 border border-slate-200 dark:border-slate-700">
            <div className="master-grid">
              {BOARD.flat().map((num, idx) => {
                const hasNumber = num > 0;
                const isCalled = hasNumber && calledSet.has(num);

                return (
                  <div
                    key={idx}
                    onClick={hasNumber ? () => handleToggleNumber(num) : undefined}
                    className={`
                      relative flex items-center justify-center
                      aspect-square text-sm sm:text-base font-bold
                      border-r border-b border-slate-200/80 dark:border-slate-700/60
                      transition-all select-none
                      ${hasNumber
                        ? isCalled
                          ? "bg-orange-500 dark:bg-orange-600 text-white cursor-pointer"
                          : "bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 cursor-pointer hover:bg-orange-50 dark:hover:bg-orange-950/20"
                        : "bg-slate-100 dark:bg-slate-900/60"
                      }
                    `}
                  >
                    {hasNumber ? num : ""}
                  </div>
                );
              })}
            </div>
          </div>
        ) : (
          <div className="text-center text-slate-400 dark:text-slate-500 py-20 text-sm">
            Nhấn &ldquo;Ván mới&rdquo; để bắt đầu
          </div>
        )}

        {/* Footer */}
        <footer className="mt-10 text-center text-xs text-slate-400 dark:text-slate-600">
          Made with ❤️ by{" "}
          <a
            href="https://miti99.com"
            target="_blank"
            rel="noopener noreferrer"
            className="text-indigo-500 hover:underline"
          >
            miti99
          </a>
        </footer>
      </div>
    </div>
  );
}

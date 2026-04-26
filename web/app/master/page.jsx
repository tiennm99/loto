"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import PlayerBoard from "@/components/player-board";

const STORAGE_KEY = "loto_master";

/**
 * @typedef {Object} MasterState
 * @property {number[]} called numbers drawn so far, in order
 * @property {number[]} remaining numbers left to draw, pre-shuffled
 */

/**
 * Build the 9x10 board: columns 0-8 map to number ranges 1-9, 10-19, ..., 80-90.
 * @returns {number[][]}
 */
function buildBoard() {
  /** @type {number[][]} */
  const board = [];
  for (let row = 0; row < 10; row++) {
    /** @type {number[]} */
    const cells = [];
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

/** @returns {MasterState} */
function createFreshState() {
  const all = Array.from({ length: 90 }, (_, i) => i + 1);
  // Shuffle
  for (let i = all.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [all[i], all[j]] = [all[j], all[i]];
  }
  return { called: [], remaining: all };
}

/** @param {MasterState} state */
function saveState(state) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

/** @returns {MasterState | null} */
function loadState() {
  const data = localStorage.getItem(STORAGE_KEY);
  if (!data) return null;
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
}

const BOARD = Object.freeze(buildBoard().map((row) => Object.freeze(row)));
const BOARD_FLAT = Object.freeze(BOARD.flatMap((r) => r));

export default function MasterPage() {
  /** @type {[MasterState | null, (s: MasterState | null) => void]} */
  const [state, setState] = useState(/** @type {MasterState | null} */ (null));
  /** @type {[number | null, (n: number | null) => void]} */
  const [lastCalled, setLastCalled] = useState(/** @type {number | null} */ (null));

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
            className="px-8 py-4 rounded-full font-semibold text-white text-lg
                       bg-gradient-to-r from-orange-500 to-red-500
                       hover:from-orange-600 hover:to-red-600
                       active:scale-95 transition-all shadow-lg shadow-orange-500/25"
          >
            Ván mới
          </button>
          {state && state.remaining.length > 0 && (
            <button
              onClick={handleDrawNext}
              className="px-10 py-4 rounded-full font-semibold text-white text-lg
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
          <div
            aria-label="Bảng theo dõi số đã xổ"
            className="rounded-2xl overflow-hidden shadow-xl shadow-slate-200/50 dark:shadow-black/30 border border-slate-200 dark:border-slate-700"
          >
            <div className="master-grid">
              {BOARD_FLAT.map((num, idx) => {
                const hasNumber = num > 0;
                const isCalled = hasNumber && calledSet.has(num);

                return (
                  <div
                    key={idx}
                    className={`
                      relative flex items-center justify-center
                      aspect-square text-sm sm:text-base font-bold
                      border-r border-b border-slate-200/80 dark:border-slate-700/60
                      transition-colors select-none
                      ${hasNumber
                        ? isCalled
                          ? "bg-orange-500 dark:bg-orange-600 text-white"
                          : "bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200"
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

        {/* Master's own playing card */}
        <div className="mt-10">
          <div className="text-center mb-4">
            <h2 className="text-lg font-bold text-slate-700 dark:text-slate-200">
              Bảng của quản trò
            </h2>
            <p className="text-xs text-slate-400 dark:text-slate-500">
              Quản trò cũng có thể chơi cùng
            </p>
          </div>
          <PlayerBoard storagePrefix="loto_master_card" />
        </div>

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

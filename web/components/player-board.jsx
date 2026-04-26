"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  generateGrid,
  getWaitingNumber,
  isRowComplete,
  loadCrossedState,
  loadGrid,
  saveCrossedState,
  saveGrid,
} from "@/lib/game-logic";

/**
 * @typedef {Object} PlayerBoardProps
 * @property {string} [storagePrefix] localStorage key prefix; allows multiple
 *   independent boards (e.g. user vs master)
 */

/** @param {PlayerBoardProps} props */
export default function PlayerBoard({ storagePrefix = "loto" } = {}) {
  /** @type {[number[][] | null, (g: number[][] | null) => void]} */
  const [grid, setGrid] = useState(/** @type {number[][] | null} */ (null));
  /** @type {[boolean[][], React.Dispatch<React.SetStateAction<boolean[][]>>]} */
  const [crossed, setCrossed] = useState(/** @type {boolean[][]} */ ([]));
  const [showCongrats, setShowCongrats] = useState(false);
  const [congratsRow, setCongratsRow] = useState(-1);
  /** @type {[string | null, (s: string | null) => void]} */
  const [toast, setToast] = useState(/** @type {string | null} */ (null));
  /** @type {React.MutableRefObject<ReturnType<typeof setTimeout> | null>} */
  const toastTimer = useRef(null);
  /** @type {React.MutableRefObject<Set<number>>} */
  const celebratedRows = useRef(new Set());
  /** @type {React.MutableRefObject<Set<number>>} */
  const notifiedWaitingRows = useRef(new Set());

  const dismissToast = useCallback(() => {
    setToast(null);
    if (toastTimer.current) {
      clearTimeout(toastTimer.current);
      toastTimer.current = null;
    }
  }, []);

  const showToast = useCallback(
    /** @param {string} msg */
    (msg) => {
      dismissToast();
      setToast(msg);
      toastTimer.current = setTimeout(() => setToast(null), 5000);
    },
    [dismissToast]
  );

  useEffect(() => {
    const savedGrid = loadGrid(storagePrefix);
    if (savedGrid) {
      setGrid(savedGrid);
      const savedCrossed =
        loadCrossedState(storagePrefix) ??
        savedGrid.map((row) => row.map(() => false));
      setCrossed(savedCrossed);
      celebratedRows.current.clear();
      notifiedWaitingRows.current.clear();
      for (let i = 0; i < savedGrid.length; i++) {
        if (isRowComplete(savedGrid, savedCrossed, i)) {
          celebratedRows.current.add(i);
        }
        if (getWaitingNumber(savedGrid, savedCrossed, i) !== null) {
          notifiedWaitingRows.current.add(i);
        }
      }
    }
  }, [storagePrefix]);

  useEffect(() => {
    if (crossed.length > 0) saveCrossedState(crossed, storagePrefix);
  }, [crossed, storagePrefix]);

  // Detect newly completed rows and waiting rows
  useEffect(() => {
    if (!grid || crossed.length === 0) return;

    // Pass 1: at most one bingo popup per render
    for (let i = 0; i < grid.length; i++) {
      if (!celebratedRows.current.has(i) && isRowComplete(grid, crossed, i)) {
        celebratedRows.current.add(i);
        notifiedWaitingRows.current.add(i);
        setCongratsRow(i + 1);
        setShowCongrats(true);
        break;
      }
    }

    // Pass 2: update waiting state for every non-celebrated row
    for (let i = 0; i < grid.length; i++) {
      if (celebratedRows.current.has(i)) continue;
      const waitNum = getWaitingNumber(grid, crossed, i);
      if (waitNum !== null && !notifiedWaitingRows.current.has(i)) {
        notifiedWaitingRows.current.add(i);
        showToast(`Chờ ${waitNum}`);
      } else if (waitNum === null && notifiedWaitingRows.current.has(i)) {
        notifiedWaitingRows.current.delete(i);
      }
    }
  }, [grid, crossed, showToast]);

  // Precompute per-row completeness so we don't call isRowComplete 81×/render
  const rowCompleteness = useMemo(() => {
    if (!grid || crossed.length === 0) return [];
    return grid.map((_, row) => isRowComplete(grid, crossed, row));
  }, [grid, crossed]);

  const handleGenerate = useCallback(() => {
    if (grid && !confirm("Bạn có muốn tạo lại bảng không?")) return;
    const newGrid = generateGrid();
    const newCrossed = newGrid.map((row) => row.map(() => false));
    setGrid(newGrid);
    setCrossed(newCrossed);
    saveGrid(newGrid, storagePrefix);
    saveCrossedState(newCrossed, storagePrefix);
    celebratedRows.current.clear();
    notifiedWaitingRows.current.clear();
    dismissToast();
  }, [grid, dismissToast, storagePrefix]);

  const handleCellClick = useCallback(
    /**
     * @param {number} row
     * @param {number} col
     */
    (row, col) => {
      setCrossed((prev) => {
        const next = prev.map((r) => [...r]);
        next[row][col] = !next[row][col];
        return next;
      });
    },
    []
  );

  return (
    <>
      <div className="flex justify-center mb-6">
        <button
          onClick={handleGenerate}
          className="px-8 py-3 rounded-full font-semibold text-white
                     bg-gradient-to-r from-indigo-500 to-purple-500
                     hover:from-indigo-600 hover:to-purple-600
                     active:scale-95 transition-all shadow-lg shadow-indigo-500/25"
        >
          Tạo bảng mới
        </button>
      </div>

      {grid ? (
        <div className="relative">
          <div
            aria-label="Bảng lô tô"
            className="rounded-2xl overflow-hidden shadow-xl shadow-slate-200/50 dark:shadow-black/30 border border-slate-200 dark:border-slate-700"
          >
            <div className="loto-grid">
              {grid.flat().map((num, idx) => {
                const row = Math.floor(idx / 9);
                const col = idx % 9;
                const hasNumber = num > 0;
                const isCrossed = hasNumber && !!crossed[row]?.[col];
                const rowComplete = hasNumber && rowCompleteness[row];

                if (!hasNumber) {
                  return (
                    <div
                      key={idx}
                      aria-hidden="true"
                      className="relative flex items-center justify-center aspect-square border-r border-b border-slate-200/80 dark:border-slate-700/60 bg-slate-50 dark:bg-slate-900/60"
                    />
                  );
                }

                return (
                  <button
                    key={idx}
                    type="button"
                    aria-label={`Số ${num}${isCrossed ? ", đã đánh dấu" : ""}`}
                    aria-pressed={isCrossed}
                    onClick={() => handleCellClick(row, col)}
                    className={`
                      relative flex items-center justify-center
                      aspect-square text-base sm:text-xl font-bold
                      border-r border-b border-slate-200/80 dark:border-slate-700/60
                      transition-all select-none cursor-pointer
                      focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-400
                      ${
                        isCrossed
                          ? rowComplete
                            ? "cell-crossed bg-emerald-100 dark:bg-emerald-900/40 text-emerald-500 dark:text-emerald-400"
                            : "cell-crossed bg-red-50 dark:bg-red-950/30 text-red-400 dark:text-red-500"
                          : "bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 hover:bg-indigo-50 dark:hover:bg-indigo-950/30 hover:text-indigo-600 dark:hover:text-indigo-400"
                      }
                    `}
                  >
                    {num}
                  </button>
                );
              })}
            </div>
          </div>

          {toast && (
            <div
              role="status"
              aria-live="polite"
              onClick={dismissToast}
              className="absolute inset-0 flex items-center justify-center pointer-events-auto cursor-pointer z-10"
            >
              <div className="px-6 py-3 rounded-2xl bg-amber-500/90 dark:bg-amber-600/90 text-white text-xl sm:text-2xl font-black shadow-xl animate-toast">
                {toast}
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="text-center text-slate-400 dark:text-slate-500 py-20 text-sm">
          Nhấn &ldquo;Tạo bảng mới&rdquo; để bắt đầu chơi
        </div>
      )}

      {showCongrats && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="congrats-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm animate-fade-in"
          onClick={() => setShowCongrats(false)}
          onKeyDown={(e) => {
            if (e.key === "Escape") setShowCongrats(false);
          }}
          tabIndex={-1}
        >
          <div
            className="relative mx-4 max-w-sm w-full rounded-3xl bg-white dark:bg-slate-800 p-8 text-center shadow-2xl animate-pop-in"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="absolute -top-6 left-1/2 -translate-x-1/2 text-6xl animate-bounce-slow">
              🎉
            </div>
            <div className="absolute top-2 left-4 text-2xl animate-spin-slow">
              ✨
            </div>
            <div className="absolute top-2 right-4 text-2xl animate-spin-slow-reverse">
              🎊
            </div>

            <h2
              id="congrats-title"
              className="mt-6 text-3xl font-black bg-gradient-to-r from-amber-500 via-pink-500 to-purple-500 bg-clip-text text-transparent"
            >
              Kinh!
            </h2>
            <p className="mt-3 text-lg text-slate-600 dark:text-slate-300">
              Hàng <span className="font-bold text-pink-500">{congratsRow}</span>{" "}
              đã đầy đủ!
            </p>
            <p className="mt-1 text-sm text-slate-400 dark:text-slate-500">
              Hãy hô to &ldquo;Kinh!&rdquo; 🎶
            </p>
            <button
              onClick={() => setShowCongrats(false)}
              className="mt-6 px-8 py-2.5 rounded-full font-semibold text-white
                         bg-gradient-to-r from-pink-500 to-purple-500
                         hover:from-pink-600 hover:to-purple-600
                         active:scale-95 transition-all shadow-lg"
            >
              Tuyệt vời! 🥳
            </button>
          </div>
        </div>
      )}
    </>
  );
}

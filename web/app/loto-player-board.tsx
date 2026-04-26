"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  generateGrid,
  getWaitingNumber,
  isRowComplete,
  loadCrossedState,
  loadGrid,
  saveCrossedState,
  saveGrid,
} from "./loto-game-logic";

interface PlayerBoardProps {
  /** localStorage key prefix; allows multiple independent boards (e.g. user vs master) */
  storagePrefix?: string;
}

export default function PlayerBoard({ storagePrefix = "loto" }: PlayerBoardProps) {
  const [grid, setGrid] = useState<number[][] | null>(null);
  const [crossed, setCrossed] = useState<boolean[][]>([]);
  const [showCongrats, setShowCongrats] = useState(false);
  const [congratsRow, setCongratsRow] = useState<number>(-1);
  const [toast, setToast] = useState<string | null>(null);
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const celebratedRows = useRef<Set<number>>(new Set());
  const notifiedWaitingRows = useRef<Set<number>>(new Set());

  const dismissToast = useCallback(() => {
    setToast(null);
    if (toastTimer.current) {
      clearTimeout(toastTimer.current);
      toastTimer.current = null;
    }
  }, []);

  const showToast = useCallback(
    (msg: string) => {
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

    for (let i = 0; i < grid.length; i++) {
      if (!celebratedRows.current.has(i) && isRowComplete(grid, crossed, i)) {
        celebratedRows.current.add(i);
        notifiedWaitingRows.current.add(i);
        setCongratsRow(i + 1);
        setShowCongrats(true);
        return;
      }
      const waitNum = getWaitingNumber(grid, crossed, i);
      if (waitNum !== null && !notifiedWaitingRows.current.has(i)) {
        notifiedWaitingRows.current.add(i);
        showToast(`Chờ ${waitNum}`);
        return;
      }
      if (
        waitNum === null &&
        notifiedWaitingRows.current.has(i) &&
        !celebratedRows.current.has(i)
      ) {
        notifiedWaitingRows.current.delete(i);
      }
    }
  }, [grid, crossed, showToast]);

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

  const handleCellClick = useCallback((row: number, col: number) => {
    setCrossed((prev) => {
      const next = prev.map((r) => [...r]);
      next[row][col] = !next[row][col];
      return next;
    });
  }, []);

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
          <div className="rounded-2xl overflow-hidden shadow-xl shadow-slate-200/50 dark:shadow-black/30 border border-slate-200 dark:border-slate-700">
            <div className="loto-grid">
              {grid.flat().map((num, idx) => {
                const row = Math.floor(idx / 9);
                const col = idx % 9;
                const hasNumber = num > 0;
                const isCrossed = hasNumber && crossed[row]?.[col];
                const rowComplete =
                  hasNumber && isRowComplete(grid, crossed, row);

                return (
                  <div
                    key={idx}
                    onClick={
                      hasNumber ? () => handleCellClick(row, col) : undefined
                    }
                    className={`
                      relative flex items-center justify-center
                      aspect-square text-base sm:text-xl font-bold
                      border-r border-b border-slate-200/80 dark:border-slate-700/60
                      transition-all select-none
                      ${
                        hasNumber
                          ? isCrossed
                            ? rowComplete
                              ? "cell-crossed bg-emerald-100 dark:bg-emerald-900/40 text-emerald-500 dark:text-emerald-400 cursor-pointer"
                              : "cell-crossed bg-red-50 dark:bg-red-950/30 text-red-400 dark:text-red-500 cursor-pointer"
                            : "bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 cursor-pointer hover:bg-indigo-50 dark:hover:bg-indigo-950/30 hover:text-indigo-600 dark:hover:text-indigo-400"
                          : "bg-slate-50 dark:bg-slate-900/60"
                      }
                    `}
                  >
                    {hasNumber ? num : ""}
                  </div>
                );
              })}
            </div>
          </div>

          {toast && (
            <div
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
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm animate-fade-in"
          onClick={() => setShowCongrats(false)}
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

            <h2 className="mt-6 text-3xl font-black bg-gradient-to-r from-amber-500 via-pink-500 to-purple-500 bg-clip-text text-transparent">
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

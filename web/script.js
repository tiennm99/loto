let numRows = 9;
let numCols = 9;
let numPerRow = 5;
let lotoKey = "loto_";

function randomNumbers(num, from, to) {
  let arr = Array.from({
    length: to - from
  }, (_, i) => from + i);
  arr.sort(() => 0.5 - Math.random());
  return arr.slice(0, num).sort();
}

function generate() {
  let node = document.getElementById("grid");
  if (node.innerHTML && !confirm("Bạn có muốn tạo lại bảng không?"))
    return;
  
  let cell = new Array(numRows).fill(0).map(() => new Array(numCols).fill(0));
  let countNumPerCol = new Array(numCols).fill(0);
  //Random cac cot co so trong tung dong
  for (let i = 0; i < numRows; i++) {
    let selectedCol = randomNumbers(numPerRow, 0, 9);
    selectedCol.forEach(col => {
      countNumPerCol[col] += 1;
      cell[i][col] = -1;
    });
  }
  
  for (let i = 0; i < numCols; i++) {
    let selectedNum = randomNumbers(countNumPerCol[i], 10 * i + 1, 10 * (i + 1) + 1);
    for (let j = 0; j < numRows; j++) {
      if (cell[j][i] == -1) {
        cell[j][i] = selectedNum.shift();
      }
    }
  }
  
  let html = '<table border="0">';
  for (let i = 0; i < numRows; i++) {
    html += "<tr>";
    for (let j = 0; j < numCols; j++) {
      let num = cell[i][j];
      let isEnabled = num > 0;
      html += `<td id="${i}_${j}" class="${isEnabled ? 'hightlight' : ''}" onClick="cellClicked('${i}_${j}')" style="text-align: center; pointer-events: ${isEnabled ? 'auto' : 'none'};">${isEnabled ? num : ""}</td>`;
    }
    html += "</tr >";
  }
  html += "</table>";
  node.innerHTML = html;
  save(lotoKey, html);
  saveGameState();
}

function supports_html5_storage() {
  try {
    return "localStorage" in window && window["localStorage"] !== null;
  } catch (e) {
    return false;
  }
}

function cellClicked(id) {
  let elem = document.getElementById(id);
  let isChecked = elem.classList.contains("crossed");
  if (isChecked)
    elem.classList.remove("crossed");
  else
    elem.classList.add("crossed");
  saveGameState();
}

function saveGameState() {
  for (let i = 0; i < numRows; i++) {
    for (let j = 0; j < numCols; j++) {
      let id = i + "_" + j;
      let elem = document.getElementById(id);
      if (elem.innerHTML == "") continue;
      let isChecked = elem.classList.contains("crossed");
      save(lotoKey + id, !isChecked);
    }
  }
}

function loadGameState() {
  for (let i = 0; i < numRows; i++) {
    for (let j = 0; j < numCols; j++) {
      let id = i + "_" + j;
      let elem = document.getElementById(id);
      if (elem.innerHTML == "") continue;
      let isChecked = load(lotoKey + id) == "true";
      if (isChecked)
        elem.classList.remove("crossed");
      else
        elem.classList.add("crossed");
    }
  }
}

function toggle(id) {
  let elem = document.getElementById(id);
  if (elem.style.display == "block")
    elem.style.display = "none";
  else
    elem.style.display = "block";
}

save = function (key, value) {};
load = function (key) {
  return null;
};

function start() {
  if (supports_html5_storage()) {
    save = function (key, value) {
      localStorage.setItem(key, value);
    };
    load = function (key) {
      return localStorage.getItem(key);
    };
  } else {
    alert("Trình duyệt của bạn không hỗ trợ localStorage!");
    return;
  }
  
  let grid = load(lotoKey);
  if (grid) {
    console.log("Load bảng đã được tạo sẵn");
    let node = document.getElementById("grid");
    node.innerHTML = grid;
    loadGameState();
  }
}

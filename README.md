//HTML code
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FINAL EXIT: MAZE ESCAPE</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="game-container">
        <div class="game-header">
            <div class="score">Score: <span id="score">0</span></div>
            <div class="level">Level: <span id="level">1</span></div>
        </div>
        
        <canvas id="gameCanvas" width="800" height="600"></canvas>
        
        <div id="pauseMenu" class="menu hidden">
            <h2>PAUSED</h2>
            <p>Press ESC to resume</p>
        </div>
        
        <div id="gameOverMenu" class="menu hidden">
            <h2>GAME OVER</h2>
            <p>Final Score: <span id="finalScore">0</span></p>
            <button id="restartBtn">Restart Level</button>
            <button id="restartGameBtn">Restart Game</button>
        </div>
        
        <div id="levelCompleteMenu" class="menu hidden">
            <h2>LEVEL COMPLETE!</h2>
            <p>Bonus: 100 points</p>
            <button id="nextLevelBtn">Next Level</button>
        </div>
        
        <div class="instructions">
            <p>Move: W/A/S/D or Arrow Keys | Pause: ESC</p>
            <p>Collect orbs to unlock the exit door!</p>
        </div>
    </div>
    
    <script src="game.js"></script>
</body>
</html>

//css
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&display=swap');

body {
    margin: 0;
    padding: 0;
    background-color: #0a0a0a;
    color: #fff;
    font-family: 'Orbitron', monospace;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    overflow: hidden;
}

.game-container {
    position: relative;
    background-color: #111;
    border: 2px solid #00ffff;
    border-radius: 10px;
    box-shadow: 0 0 20px #00ffff, inset 0 0 20px rgba(0, 255, 255, 0.1);
}

.game-header {
    display: flex;
    justify-content: space-between;
    padding: 10px 20px;
    background-color: rgba(0, 0, 0, 0.7);
    border-bottom: 1px solid #00ffff;
}

.score, .level {
    font-size: 18px;
    font-weight: 700;
    text-shadow: 0 0 10px #00ffff;
}

#gameCanvas {
    display: block;
    background-color: #050505;
}

.menu {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background-color: rgba(0, 0, 0, 0.9);
    border: 2px solid #ff00ff;
    border-radius: 10px;
    padding: 30px;
    text-align: center;
    box-shadow: 0 0 30px #ff00ff;
    z-index: 10;
}

.menu h2 {
    margin-top: 0;
    font-size: 36px;
    font-weight: 900;
    text-shadow: 0 0 20px #ff00ff;
    margin-bottom: 20px;
}

.menu p {
    font-size: 18px;
    margin: 10px 0;
}

button {
    background-color: #ff00ff;
    color: #fff;
    border: none;
    padding: 12px 24px;
    margin: 10px;
    border-radius: 5px;
    font-family: 'Orbitron', monospace;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.3s;
    text-transform: uppercase;
}

button:hover {
    background-color: #ff00ff;
    box-shadow: 0 0 15px #ff00ff;
    transform: scale(1.05);
}

.hidden {
    display: none;
}

.instructions {
    padding: 10px 20px;
    background-color: rgba(0, 0, 0, 0.7);
    border-top: 1px solid #00ffff;
    font-size: 14px;
    text-align: center;
}

.instructions p {
    margin: 5px 0;
    color: #00ffff;
}

//js
// Game constants
const CELL_SIZE = 40;
const MAZE_WIDTH = 20;
const MAZE_HEIGHT = 15;
const CANVAS_WIDTH = MAZE_WIDTH * CELL_SIZE;
const CANVAS_HEIGHT = MAZE_HEIGHT * CELL_SIZE;

// Game state
const game = {
    canvas: null,
    ctx: null,
    score: 0,
    level: 1,
    isPaused: false,
    isGameOver: false,
    isLevelComplete: false,
    player: null,
    enemy: null,
    orbs: [],
    powerOrbs: [],
    exitDoor: null,
    exitUnlocked: false,
    enemyFrozen: false,
    enemyFrozenTimer: 0,
    heartbeatTimer: 0,
    heartbeatPlaying: false,
    keys: {},
    lastTime: 0,
    animationFrame: null
};

// Audio context for sound effects
const audioContext = new (window.AudioContext || window.webkitAudioContext)();

// Sound effect functions
function playSound(frequency, duration, type = 'sine') {
    const oscillator = audioContext.createOscillator();
    const gainNode = audioContext.createGain();
    
    oscillator.connect(gainNode);
    gainNode.connect(audioContext.destination);
    
    oscillator.type = type;
    oscillator.frequency.value = frequency;
    
    gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + duration);
    
    oscillator.start(audioContext.currentTime);
    oscillator.stop(audioContext.currentTime + duration);
}

function playOrbSound() {
    playSound(800, 0.1);
    setTimeout(() => playSound(1000, 0.1), 50);
}

function playPowerOrbSound() {
    playSound(600, 0.2);
    setTimeout(() => playSound(800, 0.2), 100);
    setTimeout(() => playSound(1000, 0.3), 200);
}

function playExitSound() {
    playSound(400, 0.3);
    setTimeout(() => playSound(600, 0.3), 150);
    setTimeout(() => playSound(800, 0.5), 300);
}

function playGameOverSound() {
    playSound(150, 0.5, 'sawtooth');
    setTimeout(() => playSound(100, 0.7, 'sawtooth'), 200);
}

function playHeartbeatSound() {
    playSound(80, 0.1, 'sine');
}

// Player class
class Player {
    constructor(x, y) {
        this.x = x;
        this.y = y;
        this.speed = 0.15;
        this.size = CELL_SIZE * 0.6;
        this.targetX = x;
        this.targetY = y;
        this.moving = false;
    }
    
    update(deltaTime) {
        if (this.moving) {
            const dx = this.targetX - this.x;
            const dy = this.targetY - this.y;
            const distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 1) {
                this.x += (dx / distance) * this.speed * deltaTime;
                this.y += (dy / distance) * this.speed * deltaTime;
            } else {
                this.x = this.targetX;
                this.y = this.targetY;
                this.moving = false;
            }
        }
    }
    
    move(dx, dy) {
        if (this.moving || game.isPaused || game.isGameOver || game.isLevelComplete) return;
        
        const newX = this.x + dx;
        const newY = this.y + dy;
        
        if (this.canMoveTo(newX, newY)) {
            this.targetX = newX;
            this.targetY = newY;
            this.moving = true;
        }
    }
    
    canMoveTo(x, y) {
        const margin = this.size / 2;
        const left = x - margin;
        const right = x + margin;
        const top = y - margin;
        const bottom = y + margin;
        
        // Check collision with walls
        for (let i = 0; i < currentLevel.walls.length; i++) {
            const wall = currentLevel.walls[i];
            if (right > wall.x && left < wall.x + wall.width &&
                bottom > wall.y && top < wall.y + wall.height) {
                return false;
            }
        }
        
        return true;
    }
    
    draw(ctx) {
        ctx.save();
        
        // Player glow effect
        ctx.shadowBlur = 20;
        ctx.shadowColor = '#00ffff';
        
        // Player body
        ctx.fillStyle = '#00ffff';
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size / 2, 0, Math.PI * 2);
        ctx.fill();
        
        // Player outline
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2;
        ctx.stroke();
        
        ctx.restore();
    }
}

// Enemy class
class Enemy {
    constructor(x, y) {
        this.x = x;
        this.y = y;
        this.speed = 0.08;
        this.size = CELL_SIZE * 0.7;
        this.targetX = x;
        this.targetY = y;
        this.moving = false;
        this.path = [];
        this.pathIndex = 0;
    }
    
    update(deltaTime) {
        if (game.enemyFrozen) {
            game.enemyFrozenTimer -= deltaTime;
            if (game.enemyFrozenTimer <= 0) {
                game.enemyFrozen = false;
            }
            return;
        }
        
        // Increase speed based on level
        const levelSpeedMultiplier = 1 + (game.level - 1) * 0.2;
        const currentSpeed = this.speed * levelSpeedMultiplier;
        
        if (!this.moving || this.pathIndex >= this.path.length) {
            this.findPath();
        }
        
        if (this.path.length > 0 && this.pathIndex < this.path.length) {
            const target = this.path[this.pathIndex];
            const dx = target.x - this.x;
            const dy = target.y - this.y;
            const distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 1) {
                this.x += (dx / distance) * currentSpeed * deltaTime;
                this.y += (dy / distance) * currentSpeed * deltaTime;
            } else {
                this.x = target.x;
                this.y = target.y;
                this.pathIndex++;
            }
        }
        
        // Check heartbeat proximity
        const playerDistance = Math.sqrt(
            Math.pow(this.x - game.player.x, 2) + 
            Math.pow(this.y - game.player.y, 2)
        );
        
        if (playerDistance < 150) {
            game.heartbeatTimer += deltaTime;
            if (game.heartbeatTimer > 1000 && !game.heartbeatPlaying) {
                playHeartbeatSound();
                game.heartbeatPlaying = true;
                game.heartbeatTimer = 0;
            }
        } else {
            game.heartbeatPlaying = false;
            game.heartbeatTimer = 0;
        }
    }
    
    findPath() {
        // Simple pathfinding - move towards player
        this.path = [];
        this.pathIndex = 0;
        
        const playerGridX = Math.floor(game.player.x / CELL_SIZE);
        const playerGridY = Math.floor(game.player.y / CELL_SIZE);
        const enemyGridX = Math.floor(this.x / CELL_SIZE);
        const enemyGridY = Math.floor(this.y / CELL_SIZE);
        
        // Direct path for higher levels (smarter AI)
        if (game.level >= 3) {
            this.path.push({ x: game.player.x, y: game.player.y });
        } else {
            // Simple movement towards player
            const dx = playerGridX - enemyGridX;
            const dy = playerGridY - enemyGridY;
            
            if (Math.abs(dx) > Math.abs(dy)) {
                this.path.push({ 
                    x: this.x + (dx > 0 ? CELL_SIZE : -CELL_SIZE), 
                    y: this.y 
                });
            } else {
                this.path.push({ 
                    x: this.x, 
                    y: this.y + (dy > 0 ? CELL_SIZE : -CELL_SIZE) 
                });
            }
        }
        
        this.moving = true;
    }
    
    draw(ctx) {
        ctx.save();
        
        // Enemy glow effect
        ctx.shadowBlur = 20;
        ctx.shadowColor = game.enemyFrozen ? '#00ff00' : '#ff00ff';
        
        // Enemy body
        ctx.fillStyle = game.enemyFrozen ? '#00ff00' : '#330033';
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size / 2, 0, Math.PI * 2);
        ctx.fill();
        
        // Enemy outline
        ctx.strokeStyle = game.enemyFrozen ? '#00ff00' : '#ff00ff';
        ctx.lineWidth = 3;
        ctx.stroke();
        
        // Enemy eyes
        if (!game.enemyFrozen) {
            ctx.fillStyle = '#ff00ff';
            const eyeOffset = this.size / 4;
            ctx.beginPath();
            ctx.arc(this.x - eyeOffset/2, this.y - eyeOffset/2, 3, 0, Math.PI * 2);
            ctx.arc(this.x + eyeOffset/2, this.y - eyeOffset/2, 3, 0, Math.PI * 2);
            ctx.fill();
        }
        
        ctx.restore();
    }
}

// Orb class
class Orb {
    constructor(x, y) {
        this.x = x;
        this.y = y;
        this.size = CELL_SIZE * 0.3;
        this.collected = false;
        this.glow = 0;
        this.glowDirection = 1;
    }
    
    update(deltaTime) {
        // Animate glow
        this.glow += this.glowDirection * 0.05 * deltaTime / 16;
        if (this.glow > 1 || this.glow < 0) {
            this.glowDirection *= -1;
        }
    }
    
    draw(ctx) {
        if (this.collected) return;
        
        ctx.save();
        
        // Glow effect
        ctx.shadowBlur = 15 + this.glow * 10;
        ctx.shadowColor = '#ffff00';
        
        // Orb body
        ctx.fillStyle = '#ffff00';
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size / 2, 0, Math.PI * 2);
        ctx.fill();
        
        // Inner glow
        ctx.fillStyle = '#ffffff';
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size / 4, 0, Math.PI * 2);
        ctx.fill();
        
        ctx.restore();
    }
}

// PowerOrb class
class PowerOrb {
    constructor(x, y) {
        this.x = x;
        this.y = y;
        this.size = CELL_SIZE * 0.4;
        this.collected = false;
        this.rotation = 0;
        this.scale = 1;
    }
    
    update(deltaTime) {
        // Animate rotation and scale
        this.rotation += 0.02 * deltaTime / 16;
        this.scale = 1 + Math.sin(this.rotation * 2) * 0.1;
    }
    
    draw(ctx) {
        if (this.collected) return;
        
        ctx.save();
        ctx.translate(this.x, this.y);
        ctx.rotate(this.rotation);
        ctx.scale(this.scale, this.scale);
        
        // Glow effect
        ctx.shadowBlur = 25;
        ctx.shadowColor = '#ff00ff';
        
        // Star shape
        ctx.fillStyle = '#ff00ff';
        ctx.beginPath();
        for (let i = 0; i < 5; i++) {
            const angle = (i * Math.PI * 2) / 5 - Math.PI / 2;
            const x = Math.cos(angle) * this.size / 2;
            const y = Math.sin(angle) * this.size / 2;
            
            if (i === 0) {
                ctx.moveTo(x, y);
            } else {
                ctx.lineTo(x, y);
            }
            
            const innerAngle = angle + Math.PI / 5;
            const innerX = Math.cos(innerAngle) * this.size / 4;
            const innerY = Math.sin(innerAngle) * this.size / 4;
            ctx.lineTo(innerX, innerY);
        }
        ctx.closePath();
        ctx.fill();
        
        ctx.restore();
    }
}

// ExitDoor class
class ExitDoor {
    constructor(x, y) {
        this.x = x;
        this.y = y;
        this.width = CELL_SIZE;
        this.height = CELL_SIZE * 1.5;
        this.unlocked = false;
        this.glow = 0;
        this.glowDirection = 1;
    }
    
    update(deltaTime) {
        // Animate glow when unlocked
        if (this.unlocked) {
            this.glow += this.glowDirection * 0.03 * deltaTime / 16;
            if (this.glow > 1 || this.glow < 0) {
                this.glowDirection *= -1;
            }
        }
    }
    
    draw(ctx) {
        ctx.save();
        
        // Glow effect
        ctx.shadowBlur = this.unlocked ? 30 + this.glow * 20 : 10;
        ctx.shadowColor = this.unlocked ? '#00ff00' : '#00ffff';
        
        // Door frame
        ctx.strokeStyle = this.unlocked ? '#00ff00' : '#00ffff';
        ctx.lineWidth = 3;
        ctx.strokeRect(this.x, this.y, this.width, this.height);
        
        // Door fill
        ctx.fillStyle = this.unlocked ? 'rgba(0, 255, 0, 0.3)' : 'rgba(0, 255, 255, 0.1)';
        ctx.fillRect(this.x, this.y, this.width, this.height);
        
        // Door handle
        ctx.fillStyle = this.unlocked ? '#00ff00' : '#00ffff';
        ctx.beginPath();
        ctx.arc(this.x + this.width * 0.7, this.y + this.height * 0.5, 5, 0, Math.PI * 2);
        ctx.fill();
        
        ctx.restore();
    }
}

// Level definitions
const levels = [
    {
        // Level 1 - Simple maze
        walls: [
            // Outer walls
            { x: 0, y: 0, width: MAZE_WIDTH * CELL_SIZE, height: CELL_SIZE },
            { x: 0, y: 0, width: CELL_SIZE, height: MAZE_HEIGHT * CELL_SIZE },
            { x: (MAZE_WIDTH - 1) * CELL_SIZE, y: 0, width: CELL_SIZE, height: MAZE_HEIGHT * CELL_SIZE },
            { x: 0, y: (MAZE_HEIGHT - 1) * CELL_SIZE, width: MAZE_WIDTH * CELL_SIZE, height: CELL_SIZE },
            
            // Inner walls
            { x: 2 * CELL_SIZE, y: 2 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 8 * CELL_SIZE, y: 2 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 14 * CELL_SIZE, y: 2 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            
            { x: 2 * CELL_SIZE, y: 4 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 4 * CELL_SIZE, y: 6 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 10 * CELL_SIZE, y: 4 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 14 * CELL_SIZE, y: 4 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            
            { x: 6 * CELL_SIZE, y: 8 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 12 * CELL_SIZE, y: 8 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            
            { x: 2 * CELL_SIZE, y: 10 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 8 * CELL_SIZE, y: 10 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 14 * CELL_SIZE, y: 10 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE }
        ],
        orbs: [
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 }
        ],
        powerOrbs: [
            { x: 10 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 }
        ],
        exitDoor: { x: 18 * CELL_SIZE, y: 6 * CELL_SIZE },
        enemy: { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
        player: { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 }
    },
    {
        // Level 2 - More complex maze with multiple fake exits
        walls: [
            // Outer walls
            { x: 0, y: 0, width: MAZE_WIDTH * CELL_SIZE, height: CELL_SIZE },
            { x: 0, y: 0, width: CELL_SIZE, height: MAZE_HEIGHT * CELL_SIZE },
            { x: (MAZE_WIDTH - 1) * CELL_SIZE, y: 0, width: CELL_SIZE, height: MAZE_HEIGHT * CELL_SIZE },
            { x: 0, y: (MAZE_HEIGHT - 1) * CELL_SIZE, width: MAZE_WIDTH * CELL_SIZE, height: CELL_SIZE },
            
            // Complex inner maze
            { x: 2 * CELL_SIZE, y: 2 * CELL_SIZE, width: CELL_SIZE, height: 6 * CELL_SIZE },
            { x: 4 * CELL_SIZE, y: 2 * CELL_SIZE, width: 8 * CELL_SIZE, height: CELL_SIZE },
            { x: 4 * CELL_SIZE, y: 4 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 6 * CELL_SIZE, y: 4 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 10 * CELL_SIZE, y: 2 * CELL_SIZE, width: CELL_SIZE, height: 6 * CELL_SIZE },
            { x: 12 * CELL_SIZE, y: 2 * CELL_SIZE, width: 6 * CELL_SIZE, height: CELL_SIZE },
            { x: 12 * CELL_SIZE, y: 4 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 14 * CELL_SIZE, y: 4 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            
            { x: 2 * CELL_SIZE, y: 8 * CELL_SIZE, width: 6 * CELL_SIZE, height: CELL_SIZE },
            { x: 2 * CELL_SIZE, y: 10 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 4 * CELL_SIZE, y: 10 * CELL_SIZE, width: 8 * CELL_SIZE, height: CELL_SIZE },
            { x: 10 * CELL_SIZE, y: 8 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 12 * CELL_SIZE, y: 8 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 14 * CELL_SIZE, y: 8 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 16 * CELL_SIZE, y: 8 * CELL_SIZE, width: 2 * CELL_SIZE, height: CELL_SIZE },
            
            { x: 6 * CELL_SIZE, y: 10 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 8 * CELL_SIZE, y: 12 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE }
        ],
        orbs: [
            // Top row
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            
            // Middle section
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            
            // Bottom section
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 }
        ],
        powerOrbs: [
            { x: 2 * CELL_SIZE + CELL_SIZE/2, y: 2 * CELL_SIZE + CELL_SIZE/2 },
            { x: 18 * CELL_SIZE + CELL_SIZE/2, y: 12 * CELL_SIZE + CELL_SIZE/2 }
        ],
        exitDoors: [
            { x: 0, y: 6 * CELL_SIZE, real: false },
            { x: 18 * CELL_SIZE, y: 6 * CELL_SIZE, real: true },
            { x: 9 * CELL_SIZE, y: 0, real: false }
        ],
        enemy: { x: 10 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
        player: { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 }
    },
    {
        // Level 3 - Fast enemy, smarter AI
        walls: [
            // Outer walls
            { x: 0, y: 0, width: MAZE_WIDTH * CELL_SIZE, height: CELL_SIZE },
            { x: 0, y: 0, width: CELL_SIZE, height: MAZE_HEIGHT * CELL_SIZE },
            { x: (MAZE_WIDTH - 1) * CELL_SIZE, y: 0, width: CELL_SIZE, height: MAZE_HEIGHT * CELL_SIZE },
            { x: 0, y: (MAZE_HEIGHT - 1) * CELL_SIZE, width: MAZE_WIDTH * CELL_SIZE, height: CELL_SIZE },
            
            // Complex maze with many dead ends
            { x: 2 * CELL_SIZE, y: 2 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 2 * CELL_SIZE, y: 4 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 4 * CELL_SIZE, y: 6 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 6 * CELL_SIZE, y: 4 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 8 * CELL_SIZE, y: 2 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 8 * CELL_SIZE, y: 4 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 10 * CELL_SIZE, y: 6 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 12 * CELL_SIZE, y: 2 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 12 * CELL_SIZE, y: 4 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 14 * CELL_SIZE, y: 6 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 16 * CELL_SIZE, y: 2 * CELL_SIZE, width: 2 * CELL_SIZE, height: CELL_SIZE },
            { x: 16 * CELL_SIZE, y: 4 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            
            { x: 2 * CELL_SIZE, y: 8 * CELL_SIZE, width: 2 * CELL_SIZE, height: CELL_SIZE },
            { x: 4 * CELL_SIZE, y: 8 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 6 * CELL_SIZE, y: 8 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 8 * CELL_SIZE, y: 8 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 10 * CELL_SIZE, y: 8 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 12 * CELL_SIZE, y: 8 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 14 * CELL_SIZE, y: 8 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 16 * CELL_SIZE, y: 8 * CELL_SIZE, width: 2 * CELL_SIZE, height: CELL_SIZE },
            
            { x: 2 * CELL_SIZE, y: 10 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 6 * CELL_SIZE, y: 10 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 8 * CELL_SIZE, y: 10 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE },
            { x: 12 * CELL_SIZE, y: 10 * CELL_SIZE, width: CELL_SIZE, height: 4 * CELL_SIZE },
            { x: 14 * CELL_SIZE, y: 10 * CELL_SIZE, width: 4 * CELL_SIZE, height: CELL_SIZE }
        ],
        orbs: [
            // Strategic placement in level 3
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 5 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 11 * CELL_SIZE + CELL_SIZE/2 },
            
            { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 3 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 5 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 7 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 9 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 11 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 13 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 15 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 17 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 },
            { x: 19 * CELL_SIZE + CELL_SIZE/2, y: 13 * CELL_SIZE + CELL_SIZE/2 }
        ],
        powerOrbs: [
            { x: 10 * CELL_SIZE + CELL_SIZE/2, y: 3 * CELL_SIZE + CELL_SIZE/2 },
            { x: 10 * CELL_SIZE + CELL_SIZE/2, y: 9 * CELL_SIZE + CELL_SIZE/2 },
            { x: 2 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 },
            { x: 18 * CELL_SIZE + CELL_SIZE/2, y: 7 * CELL_SIZE + CELL_SIZE/2 }
        ],
        exitDoor: { x: 9 * CELL_SIZE, y: 6 * CELL_SIZE },
        enemy: { x: 10 * CELL_SIZE + CELL_SIZE/2, y: 10 * CELL_SIZE + CELL_SIZE/2 },
        player: { x: 1 * CELL_SIZE + CELL_SIZE/2, y: 1 * CELL_SIZE + CELL_SIZE/2 }
    }
];

let currentLevel = null;

// Initialize game
function initGame() {
    game.canvas = document.getElementById('gameCanvas');
    game.ctx = game.canvas.getContext('2d');
    
    // Set canvas size
    game.canvas.width = CANVAS_WIDTH;
    game.canvas.height = CANVAS_HEIGHT;
    
    // Load level 1
    loadLevel(1);
    
    // Setup event listeners
    setupEventListeners();
    
    // Start game loop
    game.lastTime = performance.now();
    gameLoop(game.lastTime);
}

// Load a specific level
function loadLevel(levelNum) {
    game.level = levelNum;
    game.isGameOver = false;
    game.isLevelComplete = false;
    game.exitUnlocked = false;
    game.enemyFrozen = false;
    game.enemyFrozenTimer = 0;
    
    // Get level data
    currentLevel = levels[levelNum - 1];
    
    // Create player
    game.player = new Player(
        currentLevel.player.x + CELL_SIZE/2,
        currentLevel.player.y + CELL_SIZE/2
    );
    
    // Create enemy
    game.enemy = new Enemy(
        currentLevel.enemy.x + CELL_SIZE/2,
        currentLevel.enemy.y + CELL_SIZE/2
    );
    
    // Create orbs
    game.orbs = [];
    currentLevel.orbs.forEach(orbData => {
        game.orbs.push(new Orb(
            orbData.x + CELL_SIZE/2,
            orbData.y + CELL_SIZE/2
        ));
    });
    
    // Create power orbs
    game.powerOrbs = [];
    currentLevel.powerOrbs.forEach(powerOrbData => {
        game.powerOrbs.push(new PowerOrb(
            powerOrbData.x + CELL_SIZE/2,
            powerOrbData.y + CELL_SIZE/2
        ));
    });
    
    // Create exit door(s)
    if (currentLevel.exitDoors) {
        // Level 2 has multiple fake exits
        game.exitDoor = null;
        currentLevel.exitDoors.forEach(doorData => {
            const door = new ExitDoor(doorData.x, doorData.y);
            door.real = doorData.real;
            if (doorData.real) {
                game.exitDoor = door;
            }
        });
    } else {
        // Levels 1 and 3 have single exit
        game.exitDoor = new ExitDoor(
            currentLevel.exitDoor.x,
            currentLevel.exitDoor.y
        );
    }
    
    // Update UI
    document.getElementById('level').textContent = game.level;
    document.getElementById('score').textContent = game.score;
    
    // Hide menus
    document.getElementById('gameOverMenu').classList.add('hidden');
    document.getElementById('levelCompleteMenu').classList.add('hidden');
}

// Setup event listeners
function setupEventListeners() {
    // Keyboard controls
    document.addEventListener('keydown', (e) => {
        game.keys[e.key.toLowerCase()] = true;
        
        // Handle movement
        if (!game.isPaused && !game.isGameOver && !game.isLevelComplete) {
            switch(e.key.toLowerCase()) {
                case 'w':
                case 'arrowup':
                    game.player.move(0, -CELL_SIZE);
                    break;
                case 's':
                case 'arrowdown':
                    game.player.move(0, CELL_SIZE);
                    break;
                case 'a':
                case 'arrowleft':
                    game.player.move(-CELL_SIZE, 0);
                    break;
                case 'd':
                case 'arrowright':
                    game.player.move(CELL_SIZE, 0);
                    break;
            }
        }
        
        // Pause menu
        if (e.key === 'Escape') {
            togglePause();
        }
    });
    
    document.addEventListener('keyup', (e) => {
        game.keys[e.key.toLowerCase()] = false;
    });
    
    // Button event listeners
    document.getElementById('restartBtn').addEventListener('click', () => {
        loadLevel(game.level);
    });
    
    document.getElementById('restartGameBtn').addEventListener('click', () => {
        game.score = 0;
        loadLevel(1);
    });
    
    document.getElementById('nextLevelBtn').addEventListener('click', () => {
        if (game.level < levels.length) {
            loadLevel(game.level + 1);
        } else {
            // Game completed
            game.score += 100; // Completion bonus
            document.getElementById('score').textContent = game.score;
            alert('Congratulations! You completed all levels!');
            loadLevel(1);
        }
    });
}

// Toggle pause menu
function togglePause() {
    game.isPaused = !game.isPaused;
    const pauseMenu = document.getElementById('pauseMenu');
    
    if (game.isPaused) {
        pauseMenu.classList.remove('hidden');
    } else {
        pauseMenu.classList.add('hidden');
    }
}

// Check collision between two circular objects
function checkCollision(obj1, obj2, radius1, radius2) {
    const dx = obj1.x - obj2.x;
    const dy = obj1.y - obj2.y;
    const distance = Math.sqrt(dx * dx + dy * dy);
    
    return distance < radius1 + radius2;
}

// Check collision between player and exit door
function checkExitCollision() {
    if (!game.exitDoor || !game.exitUnlocked) return;
    
    const playerRadius = game.player.size / 2;
    const doorWidth = game.exitDoor.width;
    const doorHeight = game.exitDoor.height;
    
    // Check if player is within door bounds
    if (game.player.x + playerRadius > game.exitDoor.x &&
        game.player.x - playerRadius < game.exitDoor.x + doorWidth &&
        game.player.y + playerRadius > game.exitDoor.y &&
        game.player.y - playerRadius < game.exitDoor.y + doorHeight) {
        
        // Level complete!
        game.score += 100; // Level completion bonus
        document.getElementById('score').textContent = game.score;
        playExitSound();
        game.isLevelComplete = true;
        document.getElementById('levelCompleteMenu').classList.remove('hidden');
    }
}

// Check collision between player and enemy
function checkEnemyCollision() {
    if (game.enemyFrozen) return;
    
    const playerRadius = game.player.size / 2;
    const enemyRadius = game.enemy.size / 2;
    
    if (checkCollision(game.player, game.enemy, playerRadius, enemyRadius)) {
        // Game over
        playGameOverSound();
        game.isGameOver = true;
        document.getElementById('finalScore').textContent = game.score;
        document.getElementById('gameOverMenu').classList.remove('hidden');
    }
}

// Check collision between player and orbs
function checkOrbCollisions() {
    const playerRadius = game.player.size / 2;
    const orbRadius = CELL_SIZE * 0.15;
    
    // Check regular orbs
    for (let i = game.orbs.length - 1; i >= 0; i--) {
        const orb = game.orbs[i];
        if (!orb.collected && checkCollision(game.player, orb, playerRadius, orbRadius)) {
            orb.collected = true;
            game.score += 10;
            document.getElementById('score').textContent = game.score;
            playOrbSound();
            
            // Check if all orbs are collected
            const allCollected = game.orbs.every(o => o.collected);
            if (allCollected && game.exitDoor) {
                game.exitUnlocked = true;
                game.exitDoor.unlocked = true;
            }
        }
    }
    
    // Check power orbs
    const powerOrbRadius = CELL_SIZE * 0.2;
    for (let i = game.powerOrbs.length - 1; i >= 0; i--) {
        const powerOrb = game.powerOrbs[i];
        if (!powerOrb.collected && checkCollision(game.player, powerOrb, playerRadius, powerOrbRadius)) {
            powerOrb.collected = true;
            game.score += 50;
            document.getElementById('score').textContent = game.score;
            playPowerOrbSound();
            
            // Freeze enemy
            game.enemyFrozen = true;
            game.enemyFrozenTimer = 3000; // 3 seconds
        }
    }
}

// Update game state
function update(deltaTime) {
    if (game.isPaused || game.isGameOver || game.isLevelComplete) return;
    
    // Update player
    game.player.update(deltaTime);
    
    // Update enemy
    game.enemy.update(deltaTime);
    
    // Update orbs
    game.orbs.forEach(orb => orb.update(deltaTime));
    
    // Update power orbs
    game.powerOrbs.forEach(powerOrb => powerOrb.update(deltaTime));
    
    // Update exit door
    if (game.exitDoor) {
        game.exitDoor.update(deltaTime);
    }
    
    // Check collisions
    checkOrbCollisions();
    checkEnemyCollision();
    checkExitCollision();
}

// Render game
function render() {
    const ctx = game.ctx;
    
    // Clear canvas
    ctx.fillStyle = '#050505';
    ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    
    // Draw walls
    ctx.fillStyle = '#0066cc';
    ctx.strokeStyle = '#00ffff';
    ctx.lineWidth = 2;
    
    currentLevel.walls.forEach(wall => {
        // Wall glow effect
        ctx.shadowBlur = 15;
        ctx.shadowColor = '#00ffff';
        
        ctx.fillRect(wall.x, wall.y, wall.width, wall.height);
        ctx.strokeRect(wall.x, wall.y, wall.width, wall.height);
    });
    
    // Reset shadow
    ctx.shadowBlur = 0;
    
    // Draw orbs
    game.orbs.forEach(orb => orb.draw(ctx));
    
    // Draw power orbs
    game.powerOrbs.forEach(powerOrb => powerOrb.draw(ctx));
    
    // Draw exit door
    if (game.exitDoor) {
        game.exitDoor.draw(ctx);
    }
    
    // Draw player
    game.player.draw(ctx);
    
    // Draw enemy
    game.enemy.draw(ctx);
}

// Game loop
function gameLoop(currentTime) {
    const deltaTime = currentTime - game.lastTime;
    game.lastTime = currentTime;
    
    // Update game state
    update(deltaTime);
    
    // Render game
    render();
    
    // Continue game loop
    game.animationFrame = requestAnimationFrame(gameLoop);
}

// Start the game when page loads
window.addEventListener('load', initGame);

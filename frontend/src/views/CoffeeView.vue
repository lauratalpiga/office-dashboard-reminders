<script setup lang="ts">
import { Button } from "agnostic-vue"
import { showToast, Toast } from "@/ts/toasts"
import { onMounted, ref } from 'vue'

interface Employee {
    id: number
    name: string
    workMail: string
    birthDate: string
    hireDate: string
}

interface CoffeeCheck {
    id?: number
    date: string
    taskKey: string
    done: boolean
    doneByEmployeeId?: number | null
    doneAt?: string | null
}

interface CoffeeTaskUI {
    key: string
    group: "MORNING" | "EVENING" | "BIWEEKLY" | "OCCASIONAL"
    label: string
}

interface CoffeeTaskStatus {
    taskKey: string
    group: "MORNING" | "EVENING" | "BIWEEKLY" | "OCCASIONAL"
    label: string
    lastDoneDate: string | null
    dueInDays: number | null
}

const TASKS: CoffeeTaskUI[] = [
    { key: "MORNING_START_MACHINE", group: "MORNING", label: "Switch on the coffee machine and let it rinse" },
    { key: "MORNING_FILL_WATER", group: "MORNING", label: "Empty the water tank and fill it with fresh water from the water dispenser" },
    { key: "MORNING_MILK_COOLER", group: "MORNING", label: "Switch on the milk cooler (button under the flap)" },
    { key: "MORNING_INSERT_MILK", group: "MORNING", label: "Insert the milk container and connect it" },

    { key: "EVENING_REMOVE_MILK", group: "EVENING", label: "Remove the milk container, put the lid on, and store it" },
    { key: "EVENING_MILK_COOLER", group: "EVENING", label: "Switch off the milk cooler (button under the flap)" },
    { key: "EVENING_CLEAN_MILK", group: "EVENING", label: "Start milk system cleaning (clock button)" },
    { key: "EVENING_STOP_MASCHINE", group: "EVENING", label: "Switch off the machine (button under the front flap) and let it rinse" },
    { key: "EVENING_EMPTY_CONTAINERS", group: "EVENING", label: "Empty the grounds container and residual water container and rinse them" },

    { key: "BIWEEKLY_CLEANING_TABLET", group: "BIWEEKLY", label: "Clean the machine with a cleaning tablet (clock icon, then drop icon)" },
    { key: "BIWEEKLY_EXTERNAL_CLEANING", group: "BIWEEKLY", label: "Clean the outside with a damp cloth and dry with a microfiber cloth" },
    { key: "BIWEEKLY_CLEAN_MILK_SYSTEM", group: "BIWEEKLY", label: "Clean the milk outlet (see instructions: Disassemble and rinse combo outlet)" },

    { key: "OCCASIONAL_CLEAN_BEAN_CONTAINER", group: "OCCASIONAL", label: "Clean the bean container if it looks dirty" },
    { key: "OCCASIONAL_CHANGE_WATER_FILTER", group: "OCCASIONAL", label: "Change the water filter (machine will notify)" },
    { key: "OCCASIONAL_DESCALING", group: "OCCASIONAL", label: "Descale (machine will notify)" },
    { key: "OCCASIONAL_CHANGE_MILK_HOSE", group: "OCCASIONAL", label: "Replace milk hose and outlet (every 3 months)" }
]

const loading = ref(false)
const dutyToday = ref<Employee | null>(null)
const dutyTomorrow = ref<Employee | null>(null)
const checklist = ref<CoffeeCheck[]>([])
const selectedDate = ref<string>(new Date().toISOString().slice(0,10))
const taskStatus = ref<CoffeeTaskStatus[]>([])

function groupTitle(group: CoffeeTaskUI["group"]) {
    if(group === "MORNING") {
        return "Morning"
    }
    if(group === "EVENING") {
        return "Evening"
    }
    if(group === "BIWEEKLY") {
        return "Every two weeks"
    }
    return "Occasional"
}

function isTaskDone(taskKey: string) : boolean {
    const item = checklist.value.find(c => c.taskKey === taskKey)
    if(item) {
        return item.done
    } else {
        return false
    }
}

async function loadDuty() {
    try {
        const parseNullableEmployee = async (res: Response): Promise<Employee | null> => {
            if (!res.ok) {
                const msg = await res.text().catch(() => "")
                throw new Error(msg || "Could not load coffee duty")
            }

            const body = await res.text()
            if (!body || body.trim() === "" || body.trim() === "null") return null

            return JSON.parse(body) as Employee
        }

        const resToday = await fetch(`${import.meta.env.VITE_API_URL}/api/coffee/duty/current`)
        dutyToday.value = await parseNullableEmployee(resToday)

        const resTomorrow = await fetch(`${import.meta.env.VITE_API_URL}/api/coffee/duty/next`)
        dutyTomorrow.value = await parseNullableEmployee(resTomorrow)

    } catch (err) {
        console.error(err)
        showToast(new Toast("Error", (err as Error).message || "Coffee duty could not be loaded"))
        dutyToday.value = null
        dutyTomorrow.value = null
    }
}

async function loadChecklistForDate(dateString: string) {

    if (!dateString || dateString.trim().length === 0) {
        checklist.value = []
        showToast(new Toast("Error", "Please select a valid date (use the date picker)"))
        return
    }

    try {
        const res = await fetch(`${import.meta.env.VITE_API_URL}/api/coffee/checklist/${dateString}`)

        if (!res.ok) {
            const message = await res.text()
            throw new Error(message || "Checklist could not be loaded")
        }
        checklist.value = await res.json()
    } catch (err) {
        console.error(err)
        showToast(new Toast("Error", (err as Error).message || "Checklist could not be loaded"))
        checklist.value = []
    }
}

async function loadTaskStatus() {
    try {
        const res = await fetch(`${import.meta.env.VITE_API_URL}/api/coffee/tasks/status`)
        if (!res.ok) {
            const message = await res.text()
            throw new Error(message || "Could not load task status")
        }
        taskStatus.value = await res.json()
    } catch (err: any) {
        console.error(err)
        showToast(new Toast("Error", err?.message || "Task status could not be loaded"))
        taskStatus.value = []
    }
}

async function reloadAll() {
    loading.value = true
    try {
        await loadDuty()
        await loadChecklistForDate(selectedDate.value)
        await loadTaskStatus()
    } finally {
        loading.value = false
    }
}

async function toggleTask(taskKey: string, done: boolean) {
    loading.value = true
    try {
        const body = {
            date: selectedDate.value,
            taskKey,
            done
        }

        const res = await fetch(`${import.meta.env.VITE_API_URL}/api/coffee/checklist/set`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        })

        if (!res.ok) {
            const message = await res.text()
            showToast(new Toast("Error", message || "Checklist could not be updated"))
            return
        }

        // backend returns the saved CoffeeCheck
        const saved: CoffeeCheck = await res.json()

        // update local list (no need to reload all)
        const index = checklist.value.findIndex(c => c.taskKey === saved.taskKey && c.date === saved.date)
        if (index !== -1) {
            checklist.value[index] = saved
        } else {
            checklist.value.push(saved)
        }

        showToast(new Toast("Success", "Checklist updated"))

        // refresh task status
        await loadTaskStatus()
    } catch (err) {
        console.error(err)
        showToast(new Toast("Error", "Checklist could not be updated"))
    } finally {
        loading.value = false
    }
}



function getStatus(taskKey: string) {
    return taskStatus.value.find(s => s.taskKey === taskKey) || null
}

function tasksByGroup(group: CoffeeTaskUI["group"]) {
    return TASKS.filter(t => t.group === group)
}

onMounted(() => {
    reloadAll()
})

</script>

<template>
    <main>
        <h3 class="coffee-title">Coffee Machine</h3>

        <!-- Duty cards -->
        <div class="cards">
            <div class="card">
                <h4>☕ Coffee Duty Today</h4>
                <p v-if="!dutyToday">It's weekend! No coffee duty</p>
                <p v-else>
                    <strong>{{ dutyToday.name }}</strong><br />
                    <span class="muted">{{ dutyToday.workMail }}</span>
                </p>
            </div>

            <div class="card">
                <h4>☕ Coffee Duty Tomorrow</h4>
                <p v-if="!dutyTomorrow">It's weekend! No coffee duty</p>
                <p v-else>
                    <strong>{{ dutyTomorrow.name }}</strong><br />
                    <span class="muted">{{ dutyTomorrow.workMail }}</span>
                </p>
            </div>

            <div class="card">
                <h4>📅 Checklist Date</h4>
                <input
                    class="date-input"
                    type="date"
                    v-model="selectedDate"
                    :disabled="loading"
                    @change="loadChecklistForDate(selectedDate)"
                />
                <div class="card-actions">
                    <Button @click="reloadAll" :disabled="loading">
                        {{ loading ? "Loading..." : "Reload" }}
                    </Button>
                </div>
            </div>
        </div>

        <!-- Checklist -->
        <div class="checklist">
            <h5>Coffee Checklist</h5>

            <p v-if="loading && checklist.length === 0">Loading checklist...</p>

            <div class="group" v-for="group in ['MORNING','EVENING','BIWEEKLY','OCCASIONAL']" :key="group">
                <h4>{{ groupTitle(group as any) }}</h4>

                <div class="task"
                     v-for="task in tasksByGroup(group as any)"
                     :key="task.key">
                    <label class="task-label">
                        <input
                            type="checkbox"
                            :checked="isTaskDone(task.key)"
                            :disabled="loading"
                            @change="toggleTask(task.key, ($event.target as HTMLInputElement).checked)"
                        />

                        <div class="task-text">
                            <div class="task-title">{{ task.label }}</div>

                            <!-- BIWEEKLY -->
                            <div v-if="task.group === 'BIWEEKLY'" class="task-meta">
                                <template v-if="getStatus(task.key)">
                                    needs to be done in <strong>{{ getStatus(task.key)!.dueInDays }}</strong> days
                                    <span v-if="getStatus(task.key)!.lastDoneDate">
                                        (last done: {{ getStatus(task.key)!.lastDoneDate }})
                                    </span>
                                </template>
                            </div>

                            <!-- OCCASIONAL -->
                            <div v-else-if="task.group === 'OCCASIONAL'" class="task-meta">
                                <template v-if="getStatus(task.key)?.lastDoneDate">
                                    last done: {{ getStatus(task.key)!.lastDoneDate }}
                                </template>
                                <template v-else>
                                    last done: never
                                </template>
                            </div>
                        </div>
                    </label>
                </div>
            </div>
        </div>
    </main>
</template>

<style scoped>
main {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
    color: #2f3a40;
}

.coffee-title {
    font-size: 1.8rem;
    margin-bottom: 1rem;
}

.cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 1.5rem;
    margin-top: 1rem;
}

.card {
    background: white;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    padding: 1rem;
    box-shadow: 0 2px 4px rgba(0,0,0,0.08);
}

.card h4 {
    color: #3a4650;
}

.card p {
    color: #4b5b64;
}

.muted {
    color: #6a7a84;
}

.date-input {
    width: 100%;
    max-width: 260px;
    padding: 0.6rem;
    border: 1px solid #ddd;
    border-radius: 4px;
    color: #4a555c;
}

.card-actions {
    margin-top: 1rem;
}

.checklist {
    margin-top: 2.5rem;
}

.checklist h5 {
    font-size: 1.25rem;
    margin-bottom: 0.75rem;
    color: #3a4650;
}


.group {
    margin-top: 1.5rem;
    background: #f5f5f5;
    border-radius: 8px;
    padding: 1rem;
}

.group h4 {
    font-size: 1.1rem;
    margin-bottom: 0.5rem;
    color: #3a4650;
}

.task {
    padding: 0.5rem 0;
}

.task-label {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
    flex-wrap: wrap;
    color: #4a555c;
}

.task-text {
    display: inline;
    min-width: 0;
}

.task-title {
    font-size: 1rem;
    line-height: 1.45;
    color: #4a555c;
    font-weight: 500;
}

.task-meta {
    margin-left: 0;

    display: inline-block;
    vertical-align: middle;

    width: auto;
    max-width: 340px;
    padding: 0.12rem 0.45rem;

    border: 1px solid #cfcfcf;
    border-radius: 999px;
    background: #efefef;

    font-size: 0.72rem;
    line-height: 1.2;
    opacity: 0.85;

    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.task-meta strong,
.task-meta span {
    display: inline;
}


@media (prefers-color-scheme: dark) {
    .card {
        background: #2d2d2d;
        border-color: #444;
    }

    .coffee-title {
        color: #e6edf3;
    }

    .card h4 {
        color: #e6edf3;
    }

    .card p {
        color: #c2ccd3;
    }

    .muted {
        color: #9aa6af;
    }

    .date-input {
        background: #1a1a1a;
        border-color: #444;
        color: #e0e0e0;
    }

    .group {
        background: #2d2d2d;
    }

    .group h4 {
        color: #e6edf3;
    }

    .checklist h5 {
        color: #e6edf3;
    }

    .task-meta {
        background: #1f1f1f;
        border-color: #444;
        color: #aeb8bf;
        opacity: 1;
    }

    .task-title {
        color: #d1d9de;
        font-weight: 500;
    }

    .task-label {
        color: #cfd8de;
    }
    .task-meta strong {
        color: #e0e0e0;
        font-weight: 600;
    }

    .task-meta span {
        color: inherit;
    }

    .task-meta {
        background: #1f1f1f;
        border-color: #444;
        color: #aeb8bf;
        opacity: 1;
    }
}
</style>

package com.example.cameraandriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

data class Data(
    val car: CarState,
    val human: HumanState
)

enum class CarState{
    EMPTY,
    PARKING
}
enum class HumanState{
    EMPTY,
    SIT_DOWN,
    LEANING_FORWARD
}
class Repository{
    private val _state = MutableStateFlow(Data(CarState.EMPTY, HumanState.EMPTY))
    val state: StateFlow<Data> = _state
    val events: Flow<Event> = state.reduce(:: reduceEvent)
    fun setHumanState(state: HumanState){
        _state.update { currentData-> currentData.copy(human = state) }
    }
    fun setCarState(state: CarState){
        _state.update { currentData-> currentData.copy(car = state) }
    }
}
public fun  <S, E> Flow<S>.reduce(operation: (old: S, new: S) -> E): Flow<E> = flow{
    var oldState: S? = null
        collect { value ->
            oldState?.let { emit(operation (it, value))}
            oldState = value
        }
}
enum class Event{
    CAR_EMPTY,
    CAR_PARKING,
    HUMAN_EMPTY,
    HUMAN_SIT_DOWN,
    HUMAN_LEANING_FORWARD,
    NONE
}

fun reduceEvent(oldState: Data, newState: Data):Event{

        if (oldState.car == CarState.PARKING && newState.car == CarState.PARKING &&
            oldState.human == HumanState.EMPTY && newState.human == HumanState.SIT_DOWN){
            //Отправить уведомление о том что человек сидит возле машины
            return Event.HUMAN_SIT_DOWN
        }
        else if (oldState.car == CarState.EMPTY && newState.car == CarState.PARKING){
            //Отправить уведомление о том что машина припарковалась
            return Event.CAR_PARKING
        }
        else if (oldState.car == CarState.PARKING && newState.car == CarState.EMPTY){
            //Отправить уведомление о том что машина покинула стоянку
            return Event.CAR_EMPTY
        }
        else if (oldState.car == CarState.PARKING && newState.car == CarState.PARKING &&
            oldState.human == HumanState.EMPTY && newState.human == HumanState.LEANING_FORWARD) {
            //Отправить уведомление о том что человек смотрит в окно авто
            return Event.HUMAN_LEANING_FORWARD
        }
    return Event.NONE
}

interface InterfaceState{
    fun setEmpty():Boolean
    fun setParking():Boolean
    fun setHumanSitDown():Boolean
    fun setHumanLeaningForward(): Boolean
}




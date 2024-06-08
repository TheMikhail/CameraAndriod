package com.example.cameraandriod

import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.protobuf.Empty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class Data(
    val car: CarState,
    val human: HumanState
)

enum class CarState{
    Empty,
    Parking
}
enum class HumanState{
    Empty,
    SitDown,
    LeaningForward
}
sealed class State {
    class Car(detectedObjects: List<DetectedObject>) : State()
    class Human(detectedObjects: List<DetectedObject>): State()
}
class Repository{
    private val _state = MutableStateFlow(Data(CarState.Empty, HumanState.Empty))
    val state: StateFlow<Data> = _state

    fun setHumanState(state: HumanState){
        _state.update { currentData-> currentData.copy(human = state) }
    }
    fun setCarState(state: CarState){
        _state.update { currentData-> currentData.copy(car = state) }
    }

}
interface InterfaceState{
    fun setEmpty():Boolean
    fun setParking():Boolean
    fun setHumanSitDown():Boolean
    fun setHumanLeaningForward(): Boolean
}



